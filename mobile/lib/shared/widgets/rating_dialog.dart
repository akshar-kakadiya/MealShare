import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/theme/app_colors.dart';
import 'package:mealshare_mobile/theme/app_theme.dart';

class RatingDialog extends StatefulWidget {
  final String pickupId;
  final String targetName;
  final String targetRole; // e.g. "Receiver" or "Donor"

  const RatingDialog({
    super.key,
    required this.pickupId,
    required this.targetName,
    required this.targetRole,
  });

  static Future<bool?> show(
    BuildContext context, {
    required String pickupId,
    required String targetName,
    required String targetRole,
  }) {
    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (_) => RatingDialog(
        pickupId: pickupId,
        targetName: targetName,
        targetRole: targetRole,
      ),
    );
  }

  @override
  State<RatingDialog> createState() => _RatingDialogState();
}

class _RatingDialogState extends State<RatingDialog> {
  int _selectedRating = 5;
  final _feedbackController = TextEditingController();
  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void dispose() {
    _feedbackController.dispose();
    super.dispose();
  }

  Future<void> _submitRating() async {
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    final client = context.read<ApiClient>();
    try {
      final feedbackText = _feedbackController.text.trim();
      final body = <String, dynamic>{
        'pickupId': widget.pickupId,
        'rating': _selectedRating,
      };
      if (feedbackText.isNotEmpty) {
        body['feedback'] = feedbackText;
      }

      await client.post(ApiConfig.ratingsUrl, body: body);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Rating submitted for ${widget.targetName}!'),
            backgroundColor: AppColors.success,
          ),
        );
        Navigator.of(context).pop(true);
      }
    } on ApiException catch (e) {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
          _errorMessage = e.message;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
          _errorMessage = 'Failed to submit rating: $e';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Row(
        children: [
          const Icon(Icons.star_rounded, color: Colors.amber, size: 28),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Text(
              'Rate ${widget.targetRole}',
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'How was your redistribution experience with ${widget.targetName}?',
              style: const TextStyle(fontSize: 14, color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.lg),

            // Star rating row
            Center(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(5, (index) {
                  final starIndex = index + 1;
                  return IconButton(
                    iconSize: 36,
                    padding: const EdgeInsets.symmetric(horizontal: 2),
                    icon: Icon(
                      starIndex <= _selectedRating ? Icons.star_rounded : Icons.star_border_rounded,
                      color: Colors.amber,
                    ),
                    onPressed: _isSubmitting
                        ? null
                        : () {
                            setState(() {
                              _selectedRating = starIndex;
                            });
                          },
                  );
                }),
              ),
            ),
            Center(
              child: Text(
                '$_selectedRating of 5 Stars',
                style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.textPrimary),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // Error message if any
            if (_errorMessage != null)
              Container(
                padding: const EdgeInsets.all(AppSpacing.sm),
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                decoration: BoxDecoration(
                  color: Colors.red.shade50,
                  borderRadius: BorderRadius.circular(AppRadius.sm),
                  border: Border.all(color: Colors.red.shade200),
                ),
                child: Row(
                  children: [
                    Icon(Icons.error_outline, size: 18, color: Colors.red.shade700),
                    const SizedBox(width: AppSpacing.sm),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: TextStyle(fontSize: 12, color: Colors.red.shade700),
                      ),
                    ),
                  ],
                ),
              ),

            // Feedback field
            TextField(
              controller: _feedbackController,
              enabled: !_isSubmitting,
              maxLines: 3,
              maxLength: 1000,
              decoration: const InputDecoration(
                labelText: 'Feedback (Optional)',
                hintText: 'Share your feedback about food condition, timing, communication...',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _isSubmitting ? null : _submitRating,
          style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
          child: _isSubmitting
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                )
              : const Text('Submit Rating'),
        ),
      ],
    );
  }
}
