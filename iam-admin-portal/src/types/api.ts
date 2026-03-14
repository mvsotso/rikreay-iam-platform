// Mirrors com.iam.platform.common.dto.ApiResponse exactly
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
  errors?: FieldError[];
  timestamp: string;
  requestId: string;
  path?: string;
}

export interface FieldError {
  field: string;
  message: string;
}

// Mirrors Spring Data Page<T>
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-indexed)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
  [key: string]: string | number | boolean | undefined;
}
