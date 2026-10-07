import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateDonationRequestDto {
  @ApiPropertyOptional({
    description: 'Optional logistical or operational notes for the donor regarding this claim',
    example: 'We have an emergency shelter feeding 30 children tonight; can collect in 20 minutes.',
    maxLength: 500,
  })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  notes?: string;
}
