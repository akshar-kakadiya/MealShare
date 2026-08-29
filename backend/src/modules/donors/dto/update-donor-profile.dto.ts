import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsNotEmpty, IsNumber, IsOptional, IsString } from 'class-validator';
import { DonorType } from '@prisma/client';

export class UpdateDonorProfileDto {
  @ApiProperty({ enum: DonorType, example: DonorType.Restaurant })
  @IsEnum(DonorType)
  @IsNotEmpty()
  type: DonorType;

  @ApiProperty({ example: 'Grand Palace Banquet' })
  @IsString()
  @IsNotEmpty()
  entityName: string;

  @ApiProperty({ example: '104 Residency Road, MG Road Zone, Bengaluru' })
  @IsString()
  @IsNotEmpty()
  addressText: string;

  @ApiProperty({ example: 12.9716 })
  @IsNumber()
  latitude: number;

  @ApiProperty({ example: 77.5946 })
  @IsNumber()
  longitude: number;
}
