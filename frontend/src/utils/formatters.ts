export const formatCurrency = (amount: number | string | null | undefined): string => {
  if (amount === null || amount === undefined || amount === "") return "₹0.00";
  const num = typeof amount === "string" ? parseFloat(amount) : amount;
  if (isNaN(num)) return "₹0.00";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(num);
};

export const formatChips = (chips: number | string | null | undefined): string => {
  if (chips === null || chips === undefined || chips === "") return "0";
  const num = typeof chips === "string" ? parseInt(chips, 10) : chips;
  if (isNaN(num)) return "0";
  return num.toLocaleString();
};

export const getInitials = (name?: string): string => {
  if (!name) return "?";
  return name.slice(0, 2).toUpperCase();
};
