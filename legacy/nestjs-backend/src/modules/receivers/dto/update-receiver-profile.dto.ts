import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsInt, IsNotEmpty, IsNumber, IsPositive, IsString, Min } from 'class-validator';
import { OrgType } from '@prisma/client';

export class UpdateReceiverProfileDto {
  @ApiProperty({ enum: OrgType, example: OrgType.Shelter })
  @IsEnum(OrgType)
  @IsNotEmpty()
  type: OrgType;

  @ApiProperty({ example: 'Hope Foundation Children Shelter' })
  @IsString()
  @IsNotEmpty()
  orgName: string;

  @ApiProperty({ example: '80G-NGO-REG-2024-8976' })
  @IsString()
  @IsNotEmpty()
  registrationNumber: string;

  @ApiProperty({ example: '42 Bannerghatta Main Road, Bengaluru' })
  @IsString()
  @IsNotEmpty()
  addressText: string;

  @ApiProperty({ example: 12.9611 })
  @IsNumber()
  serviceLatitude: number;

  @ApiProperty({ example: 77.6012 })
  @IsNumber()
  serviceLongitude: number;

  @ApiProperty({ example: 10000.0, description: 'Service radius in meters' })
  @IsNumber()
  @IsPositive()
  serviceRadiusMeters: number;

  @ApiProperty({ example: 80, description: 'Daily meal beneficiary capacity' })
  @IsInt()
  @Min(1)
  beneficiaryCapacity: number;
}
