import { ApiProperty } from '@nestjs/swagger';

export class ResponseMetaDto {
  @ApiProperty({ example: 'a8f5b2c1-3d4e-4f6a-8b9c-0e1f2a3b4c5d' })
  requestId: string;

  @ApiProperty({ example: '2026-08-29T16:45:00.000Z' })
  timestamp: string;

  @ApiProperty({ required: false, example: 1 })
  page?: number;

  @ApiProperty({ required: false, example: 20 })
  pageSize?: number;

  @ApiProperty({ required: false, example: 100 })
  totalItems?: number;

  @ApiProperty({ required: false, example: 5 })
  totalPages?: number;

  @ApiProperty({ required: false, example: true })
  hasNext?: boolean;

  @ApiProperty({ required: false, example: false })
  hasPrev?: boolean;
}

export class ApiResponseDto<T> {
  @ApiProperty({ example: true })
  success: boolean;

  @ApiProperty()
  data: T;

  @ApiProperty({ type: ResponseMetaDto })
  meta: ResponseMetaDto;
}

export class ApiErrorDetailsDto {
  @ApiProperty({ example: 'ERR-AUTH-001' })
  code: string;

  @ApiProperty({ example: 'Invalid credentials or validation failure.' })
  message: string;

  @ApiProperty({ required: false })
  details?: any;
}

export class ApiErrorResponseDto {
  @ApiProperty({ example: false })
  success: boolean;

  @ApiProperty({ type: ApiErrorDetailsDto })
  error: ApiErrorDetailsDto;

  @ApiProperty({ type: ResponseMetaDto })
  meta: ResponseMetaDto;
}
