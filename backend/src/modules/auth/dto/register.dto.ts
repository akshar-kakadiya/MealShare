import { ApiProperty } from '@nestjs/swagger';
import {
  IsEmail,
  IsEnum,
  IsNotEmpty,
  IsPhoneNumber,
  IsString,
  MinLength,
  Matches,
} from 'class-validator';
import { UserRole } from '@prisma/client';

export class RegisterDto {
  @ApiProperty({ example: 'donor.restaurant@example.com', description: 'User email address' })
  @IsEmail({}, { message: 'Invalid email address format' })
  @IsNotEmpty({ message: 'Email is required' })
  email: string;

  @ApiProperty({ example: '+919876543210', description: 'E.164 phone number' })
  @IsString()
  @IsNotEmpty({ message: 'Phone number is required' })
  @Matches(/^\+[1-9]\d{1,14}$/, {
    message: 'Phone number must be in E.164 international format (e.g. +919876543210)',
  })
  phone: string;

  @ApiProperty({ example: 'SecurePassword123!', description: 'Minimum 8 characters with upper, lower, digit and special char' })
  @IsString()
  @MinLength(8, { message: 'Password must be at least 8 characters long' })
  @Matches(/((?=.*\d)|(?=.*\W+))(?![.\n])(?=.*[A-Z])(?=.*[a-z]).*$/, {
    message: 'Password must contain at least 1 uppercase letter, 1 lowercase letter, and 1 number or special character',
  })
  password: string;

  @ApiProperty({ example: 'Grand Palace Banquet', description: 'Full name or Business Entity name' })
  @IsString()
  @IsNotEmpty({ message: 'Name is required' })
  name: string;

  @ApiProperty({
    enum: [UserRole.Donor, UserRole.Receiver_Org, UserRole.Receiver_Vol],
    example: UserRole.Donor,
    description: 'Initial role to register as (Admin cannot be self-registered)',
  })
  @IsEnum(UserRole, { message: 'Role must be Donor, Receiver_Org, or Receiver_Vol' })
  role: UserRole;
}
