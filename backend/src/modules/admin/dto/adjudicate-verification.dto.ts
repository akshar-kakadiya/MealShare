import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsNotEmpty, IsOptional, IsString } from 'class-validator';
import { VerificationStatus } from '@prisma/client';

export class AdjudicateVerificationDto {
  @ApiProperty({
    enum: [VerificationStatus.Verified, VerificationStatus.Rejected],
    example: VerificationStatus.Verified,
  })
  @IsEnum(VerificationStatus)
  @IsNotEmpty()
  decision: VerificationStatus;

  @ApiProperty({ example: '80G registration certificate verified against government portal.' })
  @IsString()
  @IsOptional()
  adminNotes?: string;
}
