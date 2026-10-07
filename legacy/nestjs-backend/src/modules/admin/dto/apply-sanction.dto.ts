import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsNotEmpty, IsString } from 'class-validator';
import { UserStatus } from '@prisma/client';

export class ApplySanctionDto {
  @ApiProperty({
    enum: [UserStatus.Active, UserStatus.Restricted, UserStatus.Suspended],
    example: UserStatus.Suspended,
  })
  @IsEnum(UserStatus)
  @IsNotEmpty()
  status: UserStatus;

  @ApiProperty({ example: 'Violated food safety reporting protocols.' })
  @IsString()
  @IsNotEmpty()
  reason: string;
}
