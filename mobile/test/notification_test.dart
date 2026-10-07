import 'package:flutter_test/flutter_test.dart';
import 'package:mealshare_mobile/mock/models.dart';
import 'package:flutter/material.dart';

void main() {
  group('AppNotification Tests', () {
    test('AppNotification.fromJson parses backend notification correctly', () {
      final json = {
        'id': 'notif-123',
        'recipientUserId': 'user-1',
        'type': 'DONATION_REQUEST_CREATED',
        'title': 'New Donation Request',
        'message': 'Hope Foundation requested 10 meals of Biryani',
        'entityType': 'DONATION_REQUEST',
        'entityId': 'req-456',
        'read': false,
        'createdAt': '2026-09-25T14:30:00Z',
      };

      final notif = AppNotification.fromJson(json);

      expect(notif.id, 'notif-123');
      expect(notif.title, 'New Donation Request');
      expect(notif.description, 'Hope Foundation requested 10 meals of Biryani');
      expect(notif.type, 'donation_request_created');
      expect(notif.isRead, false);
      expect(notif.icon, Icons.assignment_outlined);
      expect(notif.timestamp, DateTime.parse('2026-09-25T14:30:00Z'));
    });

    test('AppNotification.fromJson handles read notification and pickup type', () {
      final json = {
        'id': 'notif-999',
        'recipientUserId': 'user-2',
        'type': 'PICKUP_COMPLETED',
        'title': 'Pickup Completed',
        'message': 'Volunteer delivered food successfully',
        'entityType': 'PICKUP',
        'entityId': 'pck-789',
        'read': true,
        'createdAt': '2026-09-25T15:00:00Z',
      };

      final notif = AppNotification.fromJson(json);

      expect(notif.id, 'notif-999');
      expect(notif.isRead, true);
      expect(notif.icon, Icons.local_shipping_outlined);
    });

    test('AppNotification copyWith updates isRead flag', () {
      final notif = AppNotification(
        id: 'n1',
        title: 'Title',
        description: 'Desc',
        timestamp: DateTime.now(),
        isRead: false,
      );

      final updated = notif.copyWith(isRead: true);
      expect(updated.isRead, true);
      expect(updated.id, 'n1');
      expect(updated.title, 'Title');
    });
  });
}
