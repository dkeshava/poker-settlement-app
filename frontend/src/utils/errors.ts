import axios from "axios";
export const errorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined;
    if (error.response?.status === 401) return "Your session has expired. Please sign in again.";
    return data?.message || "The request could not be completed. Please try again.";
  }
  return error instanceof Error ? error.message : "Something went wrong.";
};
