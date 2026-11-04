import axiosInstance from "./axiosInstance";
import type {
    User,
    LoginRequest,
    RegisterRequest
} from "../types/auth";

export const loginUser = async (
    credentials: LoginRequest
): Promise<string> => {

    const response = await axiosInstance.post<string>(
        "/login",
        credentials,
        {
            responseType: "text",
        }
    );

    return response.data;
};

export const registerUser = async (
    details: RegisterRequest
): Promise<User> => {

    const response = await axiosInstance.post<User>(
        "/register",
        details
    );

    return response.data;
};