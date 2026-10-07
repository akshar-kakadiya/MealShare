import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';

class TimelineWidget extends StatelessWidget {
  final List<String> steps;
  final int currentStep;

  const TimelineWidget({
    super.key,
    required this.steps,
    required this.currentStep,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(steps.length, (index) {
        final isCompleted = index < currentStep;
        final isCurrent = index == currentStep;
        final isLast = index == steps.length - 1;

        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Timeline indicator column
            SizedBox(
              width: 32,
              child: Column(
                children: [
                  // Circle/Check icon
                  Container(
                    width: 28,
                    height: 28,
                    decoration: BoxDecoration(
                      color: isCompleted
                          ? AppColors.success
                          : isCurrent
                              ? AppColors.primary
                              : AppColors.surfaceVariant,
                      shape: BoxShape.circle,
                      border: isCurrent
                          ? Border.all(color: AppColors.primary, width: 2.5)
                          : null,
                    ),
                    child: Icon(
                      isCompleted
                          ? Icons.check
                          : isCurrent
                              ? Icons.circle
                              : Icons.circle_outlined,
                      size: isCompleted ? 16 : (isCurrent ? 10 : 14),
                      color: isCompleted || isCurrent
                          ? Colors.white
                          : AppColors.textHint,
                    ),
                  ),
                  // Connecting line
                  if (!isLast)
                    Container(
                      width: 2.5,
                      height: 36,
                      color: isCompleted ? AppColors.success : AppColors.borderLight,
                    ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            // Step label
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 4, bottom: 16),
                child: Text(
                  steps[index],
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    fontWeight: isCurrent ? FontWeight.w600 : FontWeight.w400,
                    color: isCompleted || isCurrent
                        ? AppColors.textPrimary
                        : AppColors.textHint,
                  ),
                ),
              ),
            ),
          ],
        );
      }),
    );
  }
}
