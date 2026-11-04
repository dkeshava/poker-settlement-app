import { useState, useEffect, useRef, type FormEvent } from "react";
import { searchUsers } from "../api/gameApi";
import { UserPlusIcon, UserIcon } from "./Icons";

interface UserOption {
  id: number;
  username: string;
}

interface UserAutocompleteProps {
  onSelect: (data: { userId?: number; username?: string }) => void;
  busy: boolean;
  disabled?: boolean;
}

export default function UserAutocomplete({
  onSelect,
  busy,
  disabled = false,
}: UserAutocompleteProps) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<UserOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserOption | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const trimmed = query.trim();

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Debounced search when query >= 2 characters
  useEffect(() => {
    if (trimmed.length < 2) {
      setSuggestions([]);
      setLoading(false);
      setHasSearched(false);
      setShowDropdown(false);
      setSelectedUser(null);
      return;
    }

    // If user edited input away from selected user, reset selection
    if (selectedUser && selectedUser.username.toLowerCase() !== trimmed.toLowerCase()) {
      setSelectedUser(null);
    }

    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const results = await searchUsers(trimmed);
        setSuggestions(results);
        setHasSearched(true);
        setShowDropdown(true);

        // If the typed query is an exact match for one of the returned users, auto-select
        const exactMatch = results.find(
          (u) => u.username.toLowerCase() === trimmed.toLowerCase()
        );
        if (exactMatch) {
          setSelectedUser(exactMatch);
        }
      } catch (err) {
        console.error("Failed to search users:", err);
        setSuggestions([]);
        setHasSearched(true);
        setShowDropdown(true);
      } finally {
        setLoading(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [trimmed]);

  // Handle clicking a suggested user: fills input, sets selected user, closes dropdown
  const handleSelectSuggestion = (user: UserOption) => {
    setQuery(user.username);
    setSelectedUser(user);
    setShowDropdown(false);
  };

  // Submit action: enabled only when a verified user is selected
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!selectedUser || busy || disabled) return;

    onSelect({
      userId: selectedUser.id,
      username: selectedUser.username,
    });

    setQuery("");
    setSelectedUser(null);
    setSuggestions([]);
    setShowDropdown(false);
    setHasSearched(false);
  };

  const isAddDisabled = !selectedUser || busy || disabled;

  return (
    <div className="user-autocomplete-wrap" ref={containerRef}>
      <form onSubmit={handleSubmit} className="autocomplete-form">
        <div className="search-input-box">
          <UserIcon size={16} className="search-icon" />
          <input
            type="text"
            className="input search-input"
            placeholder="Search username (min 2 characters)..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => {
              if (trimmed.length >= 2 && hasSearched) {
                setShowDropdown(true);
              }
            }}
            disabled={disabled || busy}
            autoComplete="off"
          />
          {loading && <div className="search-spinner" />}
        </div>

        <button
          type="submit"
          className={`btn btn-primary btn-add-user ${isAddDisabled ? "btn-faded" : ""}`}
          disabled={isAddDisabled}
          title={isAddDisabled ? "Select a matching user from suggestions to add" : "Add player to game"}
        >
          <UserPlusIcon size={16} />
          <span>{busy ? "Adding…" : "Add Player"}</span>
        </button>
      </form>

      {/* Dropdown Suggestions */}
      {showDropdown && !loading && (
        <div className="autocomplete-dropdown">
          {suggestions.length > 0 ? (
            <div className="autocomplete-list">
              {suggestions.map((u) => {
                const isSelected = selectedUser?.id === u.id;
                return (
                  <button
                    key={u.id}
                    type="button"
                    className={`autocomplete-item ${isSelected ? "autocomplete-item-active" : ""}`}
                    onClick={() => handleSelectSuggestion(u)}
                  >
                    <div className="item-avatar">
                      {u.username.slice(0, 2).toUpperCase()}
                    </div>
                    <div className="item-info">
                      <span className="item-username">{u.username}</span>
                      <span className="item-id">ID: #{u.id}</span>
                    </div>
                    <span className="item-action">
                      {isSelected ? "Selected ✓" : "Select"}
                    </span>
                  </button>
                );
              })}
            </div>
          ) : (
            hasSearched && (
              <div className="autocomplete-empty">
                No user matching "{trimmed}"
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}
