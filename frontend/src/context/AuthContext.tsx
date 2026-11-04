import {
    createContext,
    useContext,
    useState,
    type ReactNode
} from "react";

import type {
    LoginRequest,
    RegisterRequest,
    AuthContextType
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

    const login = async (credentials: LoginRequest) => {

        const token = await loginUser(credentials);

        if (!token.trim()) {
            throw new Error("Empty authentication token received");
        }

        localStorage.setItem("token", token.trim());
        setIsAuthenticated(true);
    };

    const register = async (details: RegisterRequest) => {
        return await registerUser(details);
    };

    const logout = () => {
        localStorage.removeItem("token");
        setIsAuthenticated(false);
    };

    return (
        <AuthContext.Provider
            value={{
                isAuthenticated,
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
        throw new Error("useAuth must be used within AuthProvider");
    }

    return context;
};