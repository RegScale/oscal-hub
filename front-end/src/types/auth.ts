export interface User {
  userId: number;
  username: string;
  email: string;
  street?: string;
  city?: string;
  state?: string;
  zip?: string;
  title?: string;
  organization?: string;
  phoneNumber?: string;
  logo?: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  userId: number;
  street?: string;
  city?: string;
  state?: string;
  zip?: string;
  title?: string;
  organization?: string;
  phoneNumber?: string;
  logo?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, email: string) => Promise<void>;
  logout: () => void;
}
