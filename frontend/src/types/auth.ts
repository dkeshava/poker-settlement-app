export interface User {
    id: number;
    username: string;
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
}

export interface AuthContextType {
    isAuthenticated: boolean;
    login: (credentials: LoginRequest) => Promise<void>;
    register: (details: RegisterRequest) => Promise<User>;
    logout: () => void;
}