import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean, IsNotEmpty, IsString } from 'class-validator';

export class PolicyAgreementDto {
  @ApiProperty({ example: true, description: 'Must be explicitly set to true' })
  @IsBoolean()
  @IsNotEmpty()
  agreeToPolicy: boolean;

  @ApiProperty({ example: 'v1.0-2026', description: 'Version of the Food Donation Policy' })
  @IsString()
  @IsNotEmpty()
  policyVersion: string;
}
