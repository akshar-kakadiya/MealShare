import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class LoginDto {
  @ApiProperty({ example: 'donor.restaurant@example.com', description: 'Registered email or phone' })
  @IsString()
  @IsNotEmpty({ message: 'Identifier (email or phone) is required' })
  identifier: string;

  @ApiProperty({ example: 'SecurePassword123!', description: 'Account password' })
  @IsString()
  @IsNotEmpty({ message: 'Password is required' })
  password: string;
}
