import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsNumber, IsOptional, Max, Min, IsBoolean, IsInt } from 'class-validator';

export class NearbyDonationsDto {
  @ApiProperty({ example: 12.9715987 })
  @IsNumber()
  @Type(() => Number)
  @Min(-90.0)
  @Max(90.0)
  latitude: number;

  @ApiProperty({ example: 77.5945627 })
  @IsNumber()
  @Type(() => Number)
  @Min(-180.0)
  @Max(180.0)
  longitude: number;

  @ApiPropertyOptional({ example: 10.0, default: 10.0, description: 'Radius in KM' })
  @IsNumber()
  @Type(() => Number)
  @IsOptional()
  @Min(0.1)
  @Max(50.0)
  radiusKm?: number = 10.0;

  @ApiPropertyOptional({ example: true })
  @IsBoolean()
  @Type(() => Boolean)
  @IsOptional()
  isVegetarian?: boolean;

  @ApiPropertyOptional({ example: 1 })
  @IsInt()
  @Type(() => Number)
  @IsOptional()
  @Min(1)
  page?: number = 1;

  @ApiPropertyOptional({ example: 20 })
  @IsInt()
  @Type(() => Number)
  @IsOptional()
  @Min(1)
  @Max(100)
  pageSize?: number = 20;
}
