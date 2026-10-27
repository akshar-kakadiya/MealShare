import test, { describe, it } from 'node:test';
import assert from 'node:assert/strict';

// Helper to simulate isTransientError logic from client.ts
function isTransientError(error) {
  if (!error) return false;
  // If error is an axios error with a response status
  if (error.response?.status) {
    const status = error.response.status;
    return status === 408 || status === 502 || status === 503 || status === 504;
  }
  // If it is an explicit network error
  if (error.isAxiosError && !error.response) {
    return true;
  }
  if (error.code === 'ECONNABORTED' || error.message?.includes('timeout') || error.message?.includes('Network Error')) {
    return true;
  }
  return false;
}

// Simulated executeWithRetry logic from client.ts
async function executeWithRetry(requestFn, options = {}) {
  const maxRetries = options.maxRetries ?? 3;
  const initialDelay = options.initialDelayMs ?? 10;
  const factor = options.backoffFactor ?? 1.5;

  let attempt = 0;
  while (true) {
    try {
      return await requestFn();
    } catch (err) {
      attempt++;
      if (attempt > maxRetries || !isTransientError(err)) {
        throw err;
      }
      const delayMs = initialDelay * Math.pow(factor, attempt - 1);
      if (options.onRetry) {
        options.onRetry(attempt, maxRetries, delayMs, err);
      }
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
}

// Simulated AuthContext profile fetching and state coordination engine
class AuthFlowCoordinator {
  constructor() {
    this.firebaseUser = null;
    this.mealshareUser = null;
    this.authError = null;
    this.connectionStatus = null;
    this.loading = false;
    this.inFlightPromise = null;
    this.latestRequestId = 0;
    this.signedOutCount = 0;
  }

  setFirebaseUser(user) {
    this.firebaseUser = user;
  }

  async firebaseSignOut() {
    this.firebaseUser = null;
    this.signedOutCount++;
  }

  async signOut() {
    this.inFlightPromise = null;
    this.latestRequestId++;
    this.mealshareUser = null;
    this.authError = null;
    this.connectionStatus = null;
    this.firebaseUser = null;
    this.signedOutCount++;
  }

  async fetchMealShareProfile(isBackgroundCheck = false, userOverride = null, mockApiCall = null) {
    const targetUser = userOverride ?? this.firebaseUser;
    if (!targetUser) {
      this.mealshareUser = null;
      this.loading = false;
      throw new Error('No authenticated user found.');
    }

    // Join existing in-flight promise if one is already running for the exact same UID
    if (this.inFlightPromise && this.inFlightPromise.uid === targetUser.uid) {
      try {
        return await this.inFlightPromise.promise;
      } catch (err) {
        if (!isBackgroundCheck && err.message) {
          this.authError = err.message;
        }
        throw err;
      }
    }

    const requestId = ++this.latestRequestId;

    const executeFetch = async () => {
      if (requestId === this.latestRequestId) {
        this.connectionStatus = {
          isConnecting: true,
          attempt: 1,
          maxAttempts: 4,
          message: 'Connecting to MealShare backend...',
        };
      }

      try {
        // Step 1: Ensure token availability before calling protected API
        const token = await targetUser.getIdToken();
        if (!token) {
          throw new Error('Unable to acquire authentication token.');
        }

        // Step 2: Call protected endpoint with retry
        const response = await executeWithRetry(
          () => mockApiCall(token),
          {
            maxRetries: 2,
            initialDelayMs: 10,
            backoffFactor: 1.2,
            onRetry: (attempt, maxRetries) => {
              if (requestId !== this.latestRequestId) return;
              this.connectionStatus = {
                isConnecting: true,
                attempt: attempt + 1,
                maxAttempts: maxRetries + 1,
                message: `Server waking up. Reconnecting (Attempt ${attempt + 1})...`,
              };
            },
          }
        );

        const profile = response.data;

        // Step 3: Validate ADMIN role
        if (profile.role !== 'ADMIN') {
          const errorMsg = 'Access denied. Only ADMIN users can access this portal.';
          if (requestId === this.latestRequestId) {
            this.mealshareUser = null;
            this.connectionStatus = null;
            this.authError = errorMsg;
          }
          await this.firebaseSignOut();
          const err = new Error(errorMsg);
          err.isRoleError = true;
          throw err;
        }

        if (profile.status === 'SUSPENDED') {
          const errorMsg = 'Access denied. Your administrator account has been suspended.';
          if (requestId === this.latestRequestId) {
            this.mealshareUser = null;
            this.connectionStatus = null;
            this.authError = errorMsg;
          }
          await this.firebaseSignOut();
          const err = new Error(errorMsg);
          err.isRoleError = true;
          throw err;
        }

        // Only commit state if this request is still the newest
        if (requestId === this.latestRequestId) {
          this.mealshareUser = profile;
          this.authError = null;
          this.connectionStatus = null;
        }
        return profile;
      } catch (err) {
        if (err.isRoleError) {
          throw err;
        }

        if (requestId === this.latestRequestId) {
          this.connectionStatus = null;
          this.mealshareUser = null;
        }

        const status = err.response?.status;
        let message = err.response?.data?.message || err.message || 'Failed to verify admin access';

        if (status === 404) {
          message = 'No administrator profile found for this account. Please verify provisioned admin access.';
          // Notice: Do NOT sign out so manual retry is possible
        } else if (status === 401) {
          message = 'Authentication verification failed or session expired. Please retry or sign in again.';
        } else if (status === 403) {
          message = 'Access denied. You do not have administrator permissions.';
        } else if (isTransientError(err)) {
          message = 'The MealShare backend is waking up or temporarily unavailable. Please retry in a few moments.';
        }

        if (requestId === this.latestRequestId) {
          if (isBackgroundCheck && (status === 404 || status === 401 || status === 403)) {
            this.authError = null;
          } else {
            this.authError = message;
          }
        }

        throw new Error(message);
      } finally {
        if (this.inFlightPromise?.requestId === requestId) {
          this.inFlightPromise = null;
        }
      }
    };

    const promise = executeFetch();
    this.inFlightPromise = { promise, uid: targetUser.uid, requestId };
    return promise;
  }
}

describe('MealShare Admin Portal Authentication Flow', () => {

  it('1. Recovers automatically from initial transient connection failure / cold start', async () => {
    const auth = new AuthFlowCoordinator();
    const mockUser = {
      uid: 'uSkzPzgPUVQBRES9AaDsF1ONqEG2',
      email: 'admin@mealshare.org',
      getIdToken: async () => 'valid-admin-id-token',
    };
    auth.setFirebaseUser(mockUser);

    let callCount = 0;
    const mockApiCall = async (token) => {
      callCount++;
      assert.strictEqual(token, 'valid-admin-id-token');
      if (callCount === 1) {
        // Simulate transient 503 during cold start
        const transientError = new Error('Service Unavailable');
        transientError.response = { status: 503 };
        throw transientError;
      }
      return {
        data: {
          id: '8ff89794-edb1-48c0-94eb-c584815263cb',
          email: 'admin@mealshare.org',
          name: 'Platform Administrator',
          role: 'ADMIN',
          status: 'ACTIVE',
        },
      };
    };

    const profile = await auth.fetchMealShareProfile(false, mockUser, mockApiCall);

    assert.strictEqual(callCount, 2, 'API should have been called twice (1 failure + 1 retry)');
    assert.strictEqual(profile.role, 'ADMIN');
    assert.strictEqual(auth.mealshareUser?.email, 'admin@mealshare.org');
    assert.strictEqual(auth.authError, null);
    assert.strictEqual(auth.connectionStatus, null);
  });

  it('2. Enforces Firebase ID token availability before protected request is sent', async () => {
    const auth = new AuthFlowCoordinator();
    let tokenAcquired = false;

    const mockUserWithTokenFailure = {
      uid: 'uSkzPzgPUVQBRES9AaDsF1ONqEG2',
      email: 'admin@mealshare.org',
      getIdToken: async () => {
        tokenAcquired = true;
        throw new Error('Network error obtaining Firebase ID token');
      },
    };
    auth.setFirebaseUser(mockUserWithTokenFailure);

    let apiEndpointHit = false;
    const mockApiCall = async () => {
      apiEndpointHit = true;
      return { data: {} };
    };

    await assert.rejects(
      async () => {
        await auth.fetchMealShareProfile(false, mockUserWithTokenFailure, mockApiCall);
      },
      /Network error obtaining Firebase ID token/
    );

    assert.strictEqual(tokenAcquired, true, 'Token acquisition should have been attempted');
    assert.strictEqual(apiEndpointHit, false, 'Protected API must NOT be called if token cannot be acquired');
    assert.strictEqual(auth.mealshareUser, null);
  });

  it('3. Handles genuine 404 missing profile without premature session destruction', async () => {
    const auth = new AuthFlowCoordinator();
    const mockUser = {
      uid: 'unregistered-uid-12345',
      email: 'unregistered@mealshare.org',
      getIdToken: async () => 'unregistered-token',
    };
    auth.setFirebaseUser(mockUser);

    const mockApiCall = async () => {
      const err = new Error('User not found');
      err.response = { status: 404, data: { message: 'User not found in database' } };
      throw err;
    };

    await assert.rejects(
      async () => {
        await auth.fetchMealShareProfile(false, mockUser, mockApiCall);
      },
      /No administrator profile found for this account\. Please verify provisioned admin access\./
    );

    // Profile state should be null and error clearly stated
    assert.strictEqual(auth.mealshareUser, null);
    assert.strictEqual(
      auth.authError,
      'No administrator profile found for this account. Please verify provisioned admin access.'
    );
    // Crucial: Firebase user must NOT be forcefully destroyed so that Retry action remains possible
    assert.strictEqual(auth.signedOutCount, 0, 'Must NOT prematurely sign out user on 404');
    assert.strictEqual(auth.firebaseUser?.email, 'unregistered@mealshare.org');
  });

  it('4. Rejects non-admin accounts and signs out immediately', async () => {
    const auth = new AuthFlowCoordinator();
    const mockDonorUser = {
      uid: 'donor-uid-999',
      email: 'donor@foodrescue.org',
      getIdToken: async () => 'donor-token',
    };
    auth.setFirebaseUser(mockDonorUser);

    const mockApiCall = async () => ({
      data: {
        id: 'donor-db-uuid',
        email: 'donor@foodrescue.org',
        name: 'Good Samaritan Hotel',
        role: 'DONOR',
        status: 'ACTIVE',
      },
    });

    await assert.rejects(
      async () => {
        await auth.fetchMealShareProfile(false, mockDonorUser, mockApiCall);
      },
      /Access denied\. Only ADMIN users can access this portal\./
    );

    assert.strictEqual(auth.mealshareUser, null);
    assert.strictEqual(auth.authError, 'Access denied. Only ADMIN users can access this portal.');
    assert.strictEqual(auth.signedOutCount, 1, 'Non-admin user MUST be signed out from admin portal');
  });

  it('5. Prevents stale failed requests from overwriting newer successful authentication', async () => {
    const auth = new AuthFlowCoordinator();
    const mockUser = {
      uid: 'uSkzPzgPUVQBRES9AaDsF1ONqEG2',
      email: 'admin@mealshare.org',
      getIdToken: async () => 'valid-token',
    };
    auth.setFirebaseUser(mockUser);

    let resolveStaleRequest;
    const staleRequestPromise = new Promise((_, reject) => {
      resolveStaleRequest = reject;
    });

    // Request 1: A slow request that takes longer and ultimately fails
    const req1ApiCall = () => staleRequestPromise;

    // Start Request 1
    const p1 = auth.fetchMealShareProfile(false, mockUser, req1ApiCall);

    // Immediately trigger Request 2 (e.g. user clicked Retry or fresh session check)
    // Manually force clear inFlightPromise to simulate a second sequential action
    auth.inFlightPromise = null;

    const req2ApiCall = async () => ({
      data: {
        id: '8ff89794-edb1-48c0-94eb-c584815263cb',
        email: 'admin@mealshare.org',
        name: 'Platform Administrator',
        role: 'ADMIN',
        status: 'ACTIVE',
      },
    });

    const p2 = auth.fetchMealShareProfile(false, mockUser, req2ApiCall);

    // Request 2 completes successfully first
    const profile2 = await p2;
    assert.strictEqual(profile2.role, 'ADMIN');
    assert.strictEqual(auth.mealshareUser?.email, 'admin@mealshare.org');
    assert.strictEqual(auth.authError, null);

    // Now, Request 1 (the older stale request) finally rejects with 404
    const lateError = new Error('Stale lookup failed');
    lateError.response = { status: 404 };
    resolveStaleRequest(lateError);

    // Wait for Request 1 rejection to be handled
    try {
      await p1;
    } catch {
      // Expected rejection of p1
    }

    // Verify that the stale failure did NOT overwrite the successful state of Request 2!
    assert.strictEqual(
      auth.mealshareUser?.email,
      'admin@mealshare.org',
      'Successful authentication must NOT be overwritten by stale failure'
    );
    assert.strictEqual(
      auth.authError,
      null,
      'Stale failure error must NOT overwrite null authError'
    );
  });

  it('6. Deduplicates concurrent in-flight requests into a single network execution', async () => {
    const auth = new AuthFlowCoordinator();
    const mockUser = {
      uid: 'uSkzPzgPUVQBRES9AaDsF1ONqEG2',
      email: 'admin@mealshare.org',
      getIdToken: async () => 'valid-token',
    };
    auth.setFirebaseUser(mockUser);

    let networkCallCount = 0;
    const mockApiCall = async () => {
      networkCallCount++;
      // Add slight delay to allow concurrency
      await new Promise((r) => setTimeout(r, 20));
      return {
        data: {
          id: '8ff89794-edb1-48c0-94eb-c584815263cb',
          email: 'admin@mealshare.org',
          name: 'Platform Administrator',
          role: 'ADMIN',
          status: 'ACTIVE',
        },
      };
    };

    // Concurrently invoke from onAuthStateChanged and signIn
    const [res1, res2] = await Promise.all([
      auth.fetchMealShareProfile(true, mockUser, mockApiCall),
      auth.fetchMealShareProfile(false, mockUser, mockApiCall),
    ]);

    assert.strictEqual(networkCallCount, 1, 'Only one network call should occur for concurrent invocations');
    assert.strictEqual(res1.email, 'admin@mealshare.org');
    assert.strictEqual(res2.email, 'admin@mealshare.org');
    assert.strictEqual(auth.mealshareUser?.role, 'ADMIN');
  });
});
