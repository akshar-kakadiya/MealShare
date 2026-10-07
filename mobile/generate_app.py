import os

base_dir = "K:/MCA - PES/Sem 3/MealShare/mobile/lib"

def write_file(path, content):
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
    print(f"Created: {path}")

# --- Models ---
write_file("mock/models.dart", """
class UserProfile {
  final String id;
  final String name;
  final String role; // 'DONOR' or 'RECEIVER'
  final String email;

  UserProfile({required this.id, required this.name, required this.role, required this.email});
}

class Donation {
  final String id;
  final String donorId;
  final String title;
  final String description;
  final String consumeBefore;
  final String storageCondition;
  final bool hasAllergens;
  final String status; // 'AVAILABLE', 'REQUESTED', 'ACCEPTED', 'PICKED_UP'
  final DateTime createdAt;

  Donation({
    required this.id,
    required this.donorId,
    required this.title,
    required this.description,
    required this.consumeBefore,
    required this.storageCondition,
    required this.hasAllergens,
    required this.status,
    required this.createdAt,
  });

  Donation copyWith({String? status}) {
    return Donation(
      id: id,
      donorId: donorId,
      title: title,
      description: description,
      consumeBefore: consumeBefore,
      storageCondition: storageCondition,
      hasAllergens: hasAllergens,
      status: status ?? this.status,
      createdAt: createdAt,
    );
  }
}

class Request {
  final String id;
  final String donationId;
  final String receiverId;
  final String status; // 'PENDING', 'APPROVED', 'REJECTED', 'COMPLETED'
  final DateTime requestedAt;

  Request({
    required this.id,
    required this.donationId,
    required this.receiverId,
    required this.status,
    required this.requestedAt,
  });

  Request copyWith({String? status}) {
    return Request(
      id: id,
      donationId: donationId,
      receiverId: receiverId,
      status: status ?? this.status,
      requestedAt: requestedAt,
    );
  }
}
""")

# --- State ---
write_file("mock/app_state.dart", """
import 'package:flutter/foundation.dart';
import 'models.dart';

class AppState extends ChangeNotifier {
  UserProfile currentUser = UserProfile(id: 'u1', name: 'John Doe', role: 'DONOR', email: 'john@example.com');

  void switchRole() {
    if (currentUser.role == 'DONOR') {
      currentUser = UserProfile(id: 'u2', name: 'Jane Smith', role: 'RECEIVER', email: 'jane@example.com');
    } else {
      currentUser = UserProfile(id: 'u1', name: 'John Doe', role: 'DONOR', email: 'john@example.com');
    }
    notifyListeners();
  }

  List<Donation> donations = [
    Donation(
      id: 'd1',
      donorId: 'u1',
      title: 'Leftover Sandwiches',
      description: '5 veg sandwiches from our event.',
      consumeBefore: 'Within 12 hours',
      storageCondition: 'Refrigerated',
      hasAllergens: true,
      status: 'AVAILABLE',
      createdAt: DateTime.now().subtract(const Duration(hours: 1)),
    ),
  ];

  List<Request> requests = [];

  void addDonation(Donation d) {
    donations.add(d);
    notifyListeners();
  }

  void requestDonation(String donationId) {
    requests.add(Request(
      id: 'r${DateTime.now().millisecondsSinceEpoch}',
      donationId: donationId,
      receiverId: currentUser.id,
      status: 'PENDING',
      requestedAt: DateTime.now(),
    ));
    // update donation status
    int idx = donations.indexWhere((d) => d.id == donationId);
    if (idx != -1) {
      donations[idx] = donations[idx].copyWith(status: 'REQUESTED');
    }
    notifyListeners();
  }

  void updateRequestStatus(String requestId, String newStatus) {
    int reqIdx = requests.indexWhere((r) => r.id == requestId);
    if (reqIdx != -1) {
      requests[reqIdx] = requests[reqIdx].copyWith(status: newStatus);
      String donationId = requests[reqIdx].donationId;
      int donIdx = donations.indexWhere((d) => d.id == donationId);
      if (donIdx != -1) {
        if (newStatus == 'APPROVED') {
          donations[donIdx] = donations[donIdx].copyWith(status: 'ACCEPTED');
        } else if (newStatus == 'REJECTED') {
          donations[donIdx] = donations[donIdx].copyWith(status: 'AVAILABLE');
        } else if (newStatus == 'COMPLETED') {
          donations[donIdx] = donations[donIdx].copyWith(status: 'PICKED_UP');
        }
      }
      notifyListeners();
    }
  }
}
""")

# --- Shared Widgets ---
write_file("shared/widgets/app_card.dart", """
import 'package:flutter/material.dart';

class AppCard extends StatelessWidget {
  final Widget child;
  final VoidCallback? onTap;

  const AppCard({super.key, required this.child, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: child,
        ),
      ),
    );
  }
}
""")

write_file("shared/widgets/status_chip.dart", """
import 'package:flutter/material.dart';

class StatusChip extends StatelessWidget {
  final String status;

  const StatusChip({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    Color color = Colors.grey;
    if (status == 'AVAILABLE' || status == 'COMPLETED') color = Colors.green;
    if (status == 'REQUESTED' || status == 'PENDING') color = Colors.orange;
    if (status == 'ACCEPTED' || status == 'APPROVED') color = Colors.blue;
    if (status == 'PICKED_UP') color = Colors.purple;
    if (status == 'REJECTED') color = Colors.red;

    return Chip(
      label: Text(status, style: const TextStyle(color: Colors.white, fontSize: 12)),
      backgroundColor: color,
    );
  }
}
""")

write_file("shared/widgets/donation_card.dart", """
import 'package:flutter/material.dart';
import '../../mock/models.dart';
import 'app_card.dart';
import 'status_chip.dart';

class DonationCard extends StatelessWidget {
  final Donation donation;
  final VoidCallback onTap;

  const DonationCard({super.key, required this.donation, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(child: Text(donation.title, style: Theme.of(context).textTheme.titleLarge)),
              StatusChip(status: donation.status),
            ],
          ),
          const SizedBox(height: 8),
          Text(donation.description, maxLines: 2, overflow: TextOverflow.ellipsis),
          const SizedBox(height: 8),
          Row(
            children: [
              const Icon(Icons.access_time, size: 16),
              const SizedBox(width: 4),
              Text('Consume: \${donation.consumeBefore}'),
            ],
          ),
        ],
      ),
    );
  }
}
""")

write_file("shared/widgets/timeline_widget.dart", """
import 'package:flutter/material.dart';

class TimelineWidget extends StatelessWidget {
  final List<String> steps;
  final int currentStep;

  const TimelineWidget({super.key, required this.steps, required this.currentStep});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(steps.length, (index) {
        bool isCompleted = index <= currentStep;
        return Row(
          children: [
            Column(
              children: [
                Icon(
                  isCompleted ? Icons.check_circle : Icons.radio_button_unchecked,
                  color: isCompleted ? Colors.green : Colors.grey,
                ),
                if (index < steps.length - 1)
                  Container(
                    width: 2,
                    height: 30,
                    color: isCompleted ? Colors.green : Colors.grey,
                  ),
              ],
            ),
            const SizedBox(width: 16),
            Text(steps[index], style: TextStyle(
              fontWeight: isCompleted ? FontWeight.bold : FontWeight.normal,
              color: isCompleted ? Colors.black : Colors.grey,
            )),
          ],
        );
      }),
    );
  }
}
""")

# --- Donor Screens ---
write_file("donor/home_screen.dart", """
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../shared/widgets/donation_card.dart';

class DonorHomeScreen extends StatelessWidget {
  const DonorHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final myDonations = state.donations.where((d) => d.donorId == state.currentUser.id).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Donor Dashboard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications),
            onPressed: () => context.push('/donor/notifications'),
          ),
          IconButton(
            icon: const Icon(Icons.person),
            onPressed: () => context.push('/donor/profile'),
          ),
        ],
      ),
      body: myDonations.isEmpty
          ? const Center(child: Text('No donations yet.'))
          : ListView.builder(
              itemCount: myDonations.length,
              itemBuilder: (ctx, i) {
                return DonationCard(
                  donation: myDonations[i],
                  onTap: () => context.push('/donor/donation/\${myDonations[i].id}'),
                );
              },
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/donor/create'),
        label: const Text('New Donation'),
        icon: const Icon(Icons.add),
      ),
    );
  }
}
""")

write_file("donor/create_donation_screen.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../mock/models.dart';
import 'package:go_router/go_router.dart';

class CreateDonationScreen extends StatefulWidget {
  const CreateDonationScreen({super.key});

  @override
  State<CreateDonationScreen> createState() => _CreateDonationScreenState();
}

class _CreateDonationScreenState extends State<CreateDonationScreen> {
  final _formKey = GlobalKey<FormState>();
  String title = '';
  String desc = '';
  String consumeBefore = '';
  String storage = 'Room Temperature';
  bool hasAllergens = false;
  bool agreeSafety = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Create Donation')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                decoration: const InputDecoration(labelText: 'Title'),
                validator: (v) => v!.isEmpty ? 'Required' : null,
                onSaved: (v) => title = v!,
              ),
              const SizedBox(height: 16),
              TextFormField(
                decoration: const InputDecoration(labelText: 'Description'),
                maxLines: 3,
                validator: (v) => v!.isEmpty ? 'Required' : null,
                onSaved: (v) => desc = v!,
              ),
              const SizedBox(height: 16),
              TextFormField(
                decoration: const InputDecoration(labelText: 'Consume Before (e.g. 12 hours)'),
                validator: (v) => v!.isEmpty ? 'Required' : null,
                onSaved: (v) => consumeBefore = v!,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: storage,
                items: ['Room Temperature', 'Refrigerated', 'Frozen']
                    .map((s) => DropdownMenuItem(value: s, child: Text(s)))
                    .toList(),
                onChanged: (v) => setState(() => storage = v!),
                decoration: const InputDecoration(labelText: 'Storage Condition'),
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                title: const Text('Contains Allergens?'),
                value: hasAllergens,
                onChanged: (v) => setState(() => hasAllergens = v),
              ),
              CheckboxListTile(
                title: const Text('I declare this food is safe to consume.'),
                value: agreeSafety,
                onChanged: (v) => setState(() => agreeSafety = v ?? false),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () {
                  if (_formKey.currentState!.validate() && agreeSafety) {
                    _formKey.currentState!.save();
                    final state = context.read<AppState>();
                    state.addDonation(Donation(
                      id: 'd\${DateTime.now().millisecondsSinceEpoch}',
                      donorId: state.currentUser.id,
                      title: title,
                      description: desc,
                      consumeBefore: consumeBefore,
                      storageCondition: storage,
                      hasAllergens: hasAllergens,
                      status: 'AVAILABLE',
                      createdAt: DateTime.now(),
                    ));
                    context.pop();
                  } else if (!agreeSafety) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Please agree to safety guidelines')),
                    );
                  }
                },
                child: const Text('Submit Donation'),
              )
            ],
          ),
        ),
      ),
    );
  }
}
""")

write_file("donor/donation_details_screen.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../shared/widgets/status_chip.dart';

class DonorDonationDetailsScreen extends StatelessWidget {
  final String id;
  const DonorDonationDetailsScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final donation = state.donations.firstWhere((d) => d.id == id);
    final reqs = state.requests.where((r) => r.donationId == id).toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Donation Details')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(donation.title, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          StatusChip(status: donation.status),
          const SizedBox(height: 16),
          Text(donation.description),
          const Divider(),
          Text('Requests', style: Theme.of(context).textTheme.titleLarge),
          ...reqs.map((r) => ListTile(
            title: Text('Receiver: \${r.receiverId}'),
            subtitle: Text('Status: \${r.status}'),
            trailing: r.status == 'PENDING' ? Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  icon: const Icon(Icons.check, color: Colors.green),
                  onPressed: () => context.read<AppState>().updateRequestStatus(r.id, 'APPROVED'),
                ),
                IconButton(
                  icon: const Icon(Icons.close, color: Colors.red),
                  onPressed: () => context.read<AppState>().updateRequestStatus(r.id, 'REJECTED'),
                ),
              ],
            ) : null,
          )),
        ],
      ),
    );
  }
}
""")

write_file("donor/notifications_screen.dart", """
import 'package:flutter/material.dart';

class DonorNotificationsScreen extends StatelessWidget {
  const DonorNotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: ListView(
        children: const [
          ListTile(
            leading: Icon(Icons.notification_important),
            title: Text('New request for your donation'),
            subtitle: Text('2 mins ago'),
          )
        ],
      ),
    );
  }
}
""")

write_file("donor/profile_screen.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../mock/app_state.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Name: \${state.currentUser.name}'),
            Text('Role: \${state.currentUser.role}'),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () {
                state.switchRole();
                if (state.currentUser.role == 'DONOR') {
                  context.go('/donor/home');
                } else {
                  context.go('/receiver/home');
                }
              },
              child: const Text('Switch Role'),
            )
          ],
        ),
      ),
    );
  }
}
""")

# --- Receiver Screens ---
write_file("receiver/home_screen.dart", """
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../shared/widgets/donation_card.dart';

class ReceiverHomeScreen extends StatelessWidget {
  const ReceiverHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final available = state.donations.where((d) => d.status == 'AVAILABLE').toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Discover Food'),
        actions: [
          IconButton(
            icon: const Icon(Icons.list_alt),
            onPressed: () => context.push('/receiver/my_requests'),
          ),
          IconButton(
            icon: const Icon(Icons.person),
            onPressed: () => context.push('/donor/profile'), // Shared profile
          ),
        ],
      ),
      body: available.isEmpty
          ? const Center(child: Text('No food available right now.'))
          : ListView.builder(
              itemCount: available.length,
              itemBuilder: (ctx, i) {
                return DonationCard(
                  donation: available[i],
                  onTap: () => context.push('/receiver/donation/\${available[i].id}'),
                );
              },
            ),
    );
  }
}
""")

write_file("receiver/donation_details_screen.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../mock/app_state.dart';
import '../shared/widgets/status_chip.dart';

class ReceiverDonationDetailsScreen extends StatelessWidget {
  final String id;
  const ReceiverDonationDetailsScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final donation = state.donations.firstWhere((d) => d.id == id);

    return Scaffold(
      appBar: AppBar(title: const Text('Food Details')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(donation.title, style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 8),
          StatusChip(status: donation.status),
          const SizedBox(height: 16),
          Text(donation.description),
          const SizedBox(height: 16),
          ListTile(
            leading: const Icon(Icons.info),
            title: Text('Storage: \${donation.storageCondition}'),
          ),
          ListTile(
            leading: const Icon(Icons.warning, color: Colors.orange),
            title: Text(donation.hasAllergens ? 'Contains allergens' : 'No common allergens'),
          ),
          const Spacer(),
          if (donation.status == 'AVAILABLE')
            ElevatedButton(
              onPressed: () {
                context.read<AppState>().requestDonation(id);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Requested successfully!')),
                );
                context.pop();
              },
              child: const Text('Request This Food'),
            )
        ],
      ),
    );
  }
}
""")

write_file("receiver/my_requests_screen.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../mock/app_state.dart';

class MyRequestsScreen extends StatelessWidget {
  const MyRequestsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final myReqs = state.requests.where((r) => r.receiverId == state.currentUser.id).toList();

    return Scaffold(
      appBar: AppBar(title: const Text('My Requests')),
      body: ListView.builder(
        itemCount: myReqs.length,
        itemBuilder: (ctx, i) {
          final req = myReqs[i];
          final don = state.donations.firstWhere((d) => d.id == req.donationId);
          return ListTile(
            title: Text(don.title),
            subtitle: Text('Status: \${req.status}'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {
              if (req.status == 'APPROVED') {
                context.push('/receiver/pickup/\${req.id}');
              }
            },
          );
        },
      ),
    );
  }
}
""")

write_file("receiver/pickup_tracking_screen.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../shared/widgets/timeline_widget.dart';
import 'package:go_router/go_router.dart';

class PickupTrackingScreen extends StatelessWidget {
  final String requestId;
  const PickupTrackingScreen({super.key, required this.requestId});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final req = state.requests.firstWhere((r) => r.id == requestId);

    int currentStep = 1; // Approved
    if (req.status == 'COMPLETED') currentStep = 2;

    return Scaffold(
      appBar: AppBar(title: const Text('Pickup Tracking')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TimelineWidget(
              steps: const ['Requested', 'Approved', 'Picked Up'],
              currentStep: currentStep,
            ),
            const SizedBox(height: 40),
            if (req.status == 'APPROVED')
              ElevatedButton(
                onPressed: () {
                  context.read<AppState>().updateRequestStatus(requestId, 'COMPLETED');
                  context.pop();
                },
                child: const Text('Mark as Picked Up'),
              )
          ],
        ),
      ),
    );
  }
}
""")

# --- Routing & Main ---
write_file("routing/router.dart", """
import 'package:go_router/go_router.dart';
import '../donor/home_screen.dart';
import '../donor/create_donation_screen.dart';
import '../donor/donation_details_screen.dart';
import '../donor/notifications_screen.dart';
import '../donor/profile_screen.dart';
import '../receiver/home_screen.dart';
import '../receiver/donation_details_screen.dart';
import '../receiver/my_requests_screen.dart';
import '../receiver/pickup_tracking_screen.dart';

final router = GoRouter(
  initialLocation: '/donor/home',
  routes: [
    GoRoute(path: '/donor/home', builder: (c, s) => const DonorHomeScreen()),
    GoRoute(path: '/donor/create', builder: (c, s) => const CreateDonationScreen()),
    GoRoute(path: '/donor/donation/:id', builder: (c, s) => DonorDonationDetailsScreen(id: s.pathParameters['id']!)),
    GoRoute(path: '/donor/notifications', builder: (c, s) => const DonorNotificationsScreen()),
    GoRoute(path: '/donor/profile', builder: (c, s) => const ProfileScreen()),
    
    GoRoute(path: '/receiver/home', builder: (c, s) => const ReceiverHomeScreen()),
    GoRoute(path: '/receiver/donation/:id', builder: (c, s) => ReceiverDonationDetailsScreen(id: s.pathParameters['id']!)),
    GoRoute(path: '/receiver/my_requests', builder: (c, s) => const MyRequestsScreen()),
    GoRoute(path: '/receiver/pickup/:id', builder: (c, s) => PickupTrackingScreen(requestId: s.pathParameters['id']!)),
  ],
);
""")

write_file("app.dart", """
import 'package:flutter/material.dart';
import 'routing/router.dart';

class MealShareApp extends StatelessWidget {
  const MealShareApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'MealShare Prototype',
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.green,
      ),
      routerConfig: router,
    );
  }
}
""")

write_file("main.dart", """
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'app.dart';
import 'mock/app_state.dart';

void main() {
  runApp(
    ChangeNotifierProvider(
      create: (_) => AppState(),
      child: const MealShareApp(),
    ),
  );
}
""")
