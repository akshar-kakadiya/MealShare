import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
} from '@nestjs/common';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Request } from 'express';
import { v4 as uuidv4 } from 'uuid';

export interface Response<T> {
  success: boolean;
  data: T;
  meta: {
    requestId: string;
    timestamp: string;
    [key: string]: any;
  };
}

@Injectable()
export class TransformInterceptor<T> implements NestInterceptor<T, Response<T>> {
  intercept(context: ExecutionContext, next: CallHandler): Observable<Response<T>> {
    const request = context.switchToHttp().getRequest<Request>();
    const requestId = (request.headers['x-request-id'] as string) || uuidv4();
    const timestamp = new Date().toISOString();

    return next.handle().pipe(
      map((resData) => {
        // If data already contains pagination meta, merge meta
        if (resData && typeof resData === 'object' && 'data' in resData && 'meta' in resData) {
          return {
            success: true,
            data: resData.data,
            meta: {
              ...resData.meta,
              requestId,
              timestamp,
            },
          };
        }

        return {
          success: true,
          data: resData,
          meta: {
            requestId,
            timestamp,
          },
        };
      }),
    );
  }
}
