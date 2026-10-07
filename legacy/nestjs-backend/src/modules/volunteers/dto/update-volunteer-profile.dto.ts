import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsNumber, IsPositive, IsString } from 'class-validator';

export class UpdateVolunteerProfileDto {
  @ApiProperty({ example: 'Rohan Sharma' })
  @IsString()
  @IsNotEmpty()
  fullName: string;

  @ApiProperty({ example: 'Software Engineer & Food Rescue Volunteer' })
  @IsString()
  @IsNotEmpty()
  profession: string;

  @ApiProperty({ example: 'Passionate about ensuring zero food waste in Indiranagar.' })
  @IsString()
  @IsNotEmpty()
  aboutBio: string;

  @ApiProperty({ example: 12.9716 })
  @IsNumber()
  serviceLatitude: number;

  @ApiProperty({ example: 77.5946 })
  @IsNumber()
  serviceLongitude: number;

  @ApiProperty({ example: 8000.0, description: 'Operating service radius in meters' })
  @IsNumber()
  @IsPositive()
  serviceRadiusMeters: number;
}
