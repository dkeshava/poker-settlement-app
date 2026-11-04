import axios from "axios";

export const errorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | string | undefined;

    if (error.response?.status === 401) {
      return "Your session has expired. Please log in again.";
    }

    if (error.response?.status === 403) {
      return "Access denied. Only the game manager can perform this action.";
    }

    if (typeof data === "string" && data.trim()) {
      return data;
    }

    if (data && typeof data === "object") {
      if (data.message && typeof data.message === "string") {
        return data.message;
      }
      if (data.error && typeof data.error === "string") {
        return data.error;
      }
    }

    if (error.message) {
      return error.message;
    }

    return "A network error occurred. Please check your connection.";
  }

  return error instanceof Error ? error.message : "An unexpected error occurred.";
};
