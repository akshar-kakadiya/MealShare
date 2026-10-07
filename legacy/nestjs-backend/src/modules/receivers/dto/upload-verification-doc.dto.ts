import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, IsUrl } from 'class-validator';

export class UploadVerificationDocDto {
  @ApiProperty({ example: '80G Tax Exemption Certificate', description: 'Document type or description' })
  @IsString()
  @IsNotEmpty()
  documentType: string;

  @ApiProperty({
    example: 'https://storage.mealshare.org/evidence/verifications/doc_80g_8976.pdf',
    description: 'S3 uploaded document URL',
  })
  @IsUrl()
  @IsNotEmpty()
  documentUrl: string;
}
