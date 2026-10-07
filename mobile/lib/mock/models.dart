import 'package:flutter/material.dart';

class UserProfile {
  final String id;
  final String name;
  final String role; // 'DONOR' or 'RECEIVER'
  final String email;
  final String phone;
  final String address;
  final String organizationType; // 'Restaurant', 'NGO', 'Community Kitchen', etc.
  final bool isVerified;
  final DateTime joinedAt;
  final String? avatarUrl;

  UserProfile({
    required this.id,
    required this.name,
    required this.role,
    required this.email,
    this.phone = '',
    this.address = '',
    this.organizationType = '',
    this.isVerified = false,
    DateTime? joinedAt,
    this.avatarUrl,
  }) : joinedAt = joinedAt ?? DateTime.now();
}

class Donation {
  final String id;
  final String donorId;
  final String donorName;
  final String title;
  final String description;
  final int quantity;
  final String unit; // 'meals', 'kg', 'portions', 'boxes'
  final String category; // 'Meals', 'Bakery', 'Fruits', 'Vegetables', 'Dairy', 'Beverages', 'Other'
  final String consumeBefore;
  final DateTime? consumeBeforeTime;
  final String storageCondition; // 'Room Temperature', 'Refrigerated', 'Frozen'
  final String packagingCondition; // 'Sealed', 'Open', 'Partially Sealed'
  final bool hasAllergens;
  final String allergenInfo;
  final bool foodSafetyDeclared;
  final DateTime? preparationTime;
  final String pickupAddress;
  final String pickupWindowStart;
  final String pickupWindowEnd;
  final String pickupInstructions;
  final String status;
  final DateTime createdAt;
  final IconData categoryIcon;
  final double? distance; // km from receiver

  Donation({
    required this.id,
    required this.donorId,
    this.donorName = '',
    required this.title,
    required this.description,
    this.quantity = 0,
    this.unit = 'meals',
    this.category = 'Meals',
    required this.consumeBefore,
    this.consumeBeforeTime,
    required this.storageCondition,
    this.packagingCondition = 'Sealed',
    required this.hasAllergens,
    this.allergenInfo = '',
    this.foodSafetyDeclared = true,
    this.preparationTime,
    this.pickupAddress = '',
    this.pickupWindowStart = '',
    this.pickupWindowEnd = '',
    this.pickupInstructions = '',
    required this.status,
    required this.createdAt,
    this.categoryIcon = Icons.restaurant,
    this.distance,
  });

  Donation copyWith({
    String? status,
    String? title,
    String? description,
    int? quantity,
    String? unit,
    String? category,
    String? consumeBefore,
    String? storageCondition,
    String? packagingCondition,
    bool? hasAllergens,
    String? allergenInfo,
    bool? foodSafetyDeclared,
    String? pickupAddress,
    String? pickupWindowStart,
    String? pickupWindowEnd,
    String? pickupInstructions,
  }) {
    return Donation(
      id: id,
      donorId: donorId,
      donorName: donorName,
      title: title ?? this.title,
      description: description ?? this.description,
      quantity: quantity ?? this.quantity,
      unit: unit ?? this.unit,
      category: category ?? this.category,
      consumeBefore: consumeBefore ?? this.consumeBefore,
      consumeBeforeTime: consumeBeforeTime,
      storageCondition: storageCondition ?? this.storageCondition,
      packagingCondition: packagingCondition ?? this.packagingCondition,
      hasAllergens: hasAllergens ?? this.hasAllergens,
      allergenInfo: allergenInfo ?? this.allergenInfo,
      foodSafetyDeclared: foodSafetyDeclared ?? this.foodSafetyDeclared,
      preparationTime: preparationTime,
      pickupAddress: pickupAddress ?? this.pickupAddress,
      pickupWindowStart: pickupWindowStart ?? this.pickupWindowStart,
      pickupWindowEnd: pickupWindowEnd ?? this.pickupWindowEnd,
      pickupInstructions: pickupInstructions ?? this.pickupInstructions,
      status: status ?? this.status,
      createdAt: createdAt,
      categoryIcon: categoryIcon,
      distance: distance,
    );
  }
}

class FoodRequest {
  final String id;
  final String donationId;
  final String? donationTitle;
  final String receiverId;
  final String receiverName;
  final int quantity;
  final String? message;
  final double? distance;
  final String status; // 'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'COMPLETED'
  final DateTime requestedAt;

  FoodRequest({
    required this.id,
    required this.donationId,
    this.donationTitle,
    required this.receiverId,
    this.receiverName = '',
    this.quantity = 0,
    this.message,
    this.distance,
    required this.status,
    required this.requestedAt,
  });

  FoodRequest copyWith({String? status, String? donationTitle}) {
    return FoodRequest(
      id: id,
      donationId: donationId,
      donationTitle: donationTitle ?? this.donationTitle,
      receiverId: receiverId,
      receiverName: receiverName,
      quantity: quantity,
      message: message,
      distance: distance,
      status: status ?? this.status,
      requestedAt: requestedAt,
    );
  }
}

// Keep backward compatibility alias
typedef Request = FoodRequest;

class AppNotification {
  final String id;
  final String title;
  final String description;
  final DateTime timestamp;
  final String type; // 'request', 'donation', 'pickup', 'system'
  final IconData icon;
  final bool isRead;

  AppNotification({
    required this.id,
    required this.title,
    required this.description,
    required this.timestamp,
    this.type = 'system',
    this.icon = Icons.notifications,
    this.isRead = false,
  });

  AppNotification copyWith({bool? isRead}) {
    return AppNotification(
      id: id,
      title: title,
      description: description,
      timestamp: timestamp,
      type: type,
      icon: icon,
      isRead: isRead ?? this.isRead,
    );
  }

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    IconData icon = Icons.notifications;
    final typeStr = (json['type'] ?? '').toString();
    if (typeStr.contains('REQUEST')) {
      icon = Icons.assignment_outlined;
    } else if (typeStr.contains('PICKUP')) {
      icon = Icons.local_shipping_outlined;
    } else if (typeStr.contains('DONATION')) {
      icon = Icons.volunteer_activism_outlined;
    }

    return AppNotification(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['message']?.toString() ?? '',
      timestamp: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString()) ?? DateTime.now()
          : DateTime.now(),
      type: typeStr.toLowerCase(),
      icon: icon,
      isRead: json['read'] == true,
    );
  }
}

class Pickup {
  final String id;
  final String donationId;
  final String donationTitle;
  final String donorName;
  final String receiverName;
  final String pickupAddress;
  final String deliveryAddress;
  final DateTime scheduledTime;
  final String status; // 'ASSIGNED', 'READY', 'PICKUP_IN_PROGRESS', 'PICKED_UP', 'DELIVERED', 'FAILED'
  final String? otpCode;

  Pickup({
    required this.id,
    required this.donationId,
    this.donationTitle = '',
    this.donorName = '',
    this.receiverName = '',
    this.pickupAddress = '',
    this.deliveryAddress = '',
    required this.scheduledTime,
    required this.status,
    this.otpCode,
  });
}

class ActivityItem {
  final String title;
  final String subtitle;
  final IconData icon;
  final DateTime time;
  final Color? iconColor;

  ActivityItem({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.time,
    this.iconColor,
  });
}
