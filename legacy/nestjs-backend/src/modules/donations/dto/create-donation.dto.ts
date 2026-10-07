import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { StorageCondition } from '@prisma/client';
import {
  IsString,
  IsInt,
  Min,
  IsEnum,
  IsISO8601,
  IsBoolean,
  IsArray,
  IsNumber,
  Max,
  IsUrl,
  IsOptional,
  ArrayNotEmpty,
  IsNotEmpty,
} from 'class-validator';

export class CreateDonationDto {
  @ApiProperty({ example: 'Vegetable Biryani & Paneer Curry' })
  @IsString()
  @IsNotEmpty()
  foodType: string;

  @ApiProperty({ example: 35 })
  @IsInt()
  @Min(1)
  quantityServings: number;

  @ApiProperty({ example: '2026-08-29T14:30:00.000Z' })
  @IsISO8601()
  prepDateTime: string;

  @ApiProperty({ enum: StorageCondition, example: 'Refrigerated' })
  @IsEnum(StorageCondition)
  storageCondition: StorageCondition;

  @ApiProperty({ example: '2026-08-29T21:00:00.000Z' })
  @IsISO8601()
  consumptionDeadline: string;

  @ApiProperty({ example: true })
  @IsBoolean()
  isVegetarian: boolean;

  @ApiProperty({ type: [String], example: ['Dairy', 'Nuts'] })
  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  allergens?: string[];

  @ApiProperty({ example: '104 Residency Road, MG Road Zone, Bengaluru' })
  @IsString()
  @IsNotEmpty()
  pickupAddress: string;

  @ApiProperty({ example: 12.9715987 })
  @IsNumber()
  @Min(-90.0)
  @Max(90.0)
  pickupLatitude: number;

  @ApiProperty({ example: 77.5945627 })
  @IsNumber()
  @Min(-180.0)
  @Max(180.0)
  pickupLongitude: number;

  @ApiProperty({ example: 'https://storage.mealshare.org/donations/photo_3fa85f64.jpg' })
  @IsUrl()
  foodPhotoUrl: string;

  @ApiPropertyOptional({ example: 'Food packed in sanitized stainless-steel trays.' })
  @IsString()
  @IsOptional()
  notes?: string;
}
