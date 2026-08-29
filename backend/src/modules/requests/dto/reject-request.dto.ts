import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, MaxLength } from 'class-validator';

export class RejectDonationRequestDto {
  @ApiPropertyOptional({
    description: 'Optional reason for rejecting this specific receiver request',
    example: 'Capacity constraint or prior pickup scheduled.',
    maxLength: 500,
  })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  rejectionReason?: string;
}
