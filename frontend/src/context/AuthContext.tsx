import {
    createContext,
    useContext,
    useState,
    type ReactNode
} from "react";

import type {
    LoginRequest,
    RegisterRequest,
    AuthContextType,
    User
} from "../types/auth";

import { loginUser, registerUser } from "../api/authApi";

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {

    const [isAuthenticated, setIsAuthenticated] = useState(
        () => !!localStorage.getItem("token")
    );

    const [username, setUsername] = useState<string | null>(
        () => localStorage.getItem("poker_username")
    );

    const login = async (credentials: LoginRequest) => {
        const token = await loginUser(credentials);

        if (!token.trim()) {
            throw new Error("Empty authentication token received");
        }

        localStorage.setItem("token", token.trim());
        localStorage.setItem("poker_username", credentials.username);
        setUsername(credentials.username);
        setIsAuthenticated(true);
    };

    const register = async (details: RegisterRequest): Promise<User> => {
        return await registerUser(details);
    };

    const logout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("poker_username");
        setUsername(null);
        setIsAuthenticated(false);
    };

    return (
        <AuthContext.Provider
            value={{
                isAuthenticated,
                username,
                login,
                register,
                logout
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
};
