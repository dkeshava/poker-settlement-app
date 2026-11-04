import { useState, useEffect, useRef, type FormEvent } from "react";
import { searchUsers } from "../api/gameApi";
import type { UserSummary } from "../types/game";
import { SearchIcon, UserPlusIcon } from "./Icons";
import { getInitials } from "../utils/formatters";

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
  const [results, setResults] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const users = await searchUsers(query.trim());
        setResults(users);
      } catch (err) {
        console.error("Failed to search users:", err);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [query]);

  // Close dropdown on click outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelectUser = (user: UserSummary) => {
    onSelect({ userId: user.id, username: user.username });
    setQuery("");
    setShowDropdown(false);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!query.trim() || busy) return;

    // Check if entered input is numeric ID or username
    const numericId = Number(query.trim());
    if (Number.isInteger(numericId) && numericId > 0) {
      onSelect({ userId: numericId });
    } else {
      onSelect({ username: query.trim() });
    }
    setQuery("");
    setShowDropdown(false);
  };

  return (
    <div className="user-autocomplete-wrap" ref={wrapperRef}>
      <form onSubmit={handleSubmit} className="autocomplete-form">
        <div className="search-input-box">
          <SearchIcon size={16} className="search-icon" />
          <input
            type="text"
            className="input search-input"
            placeholder="Search username or enter ID (e.g. alex, 5)..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setShowDropdown(true);
            }}
            onFocus={() => setShowDropdown(true)}
            disabled={disabled || busy}
          />
          {loading && <span className="search-spinner" />}
        </div>
        <button
          type="submit"
          className="btn btn-primary btn-add-user"
          disabled={!query.trim() || busy || disabled}
        >
          <UserPlusIcon size={16} />
          <span>{busy ? "Adding…" : "Add Player"}</span>
        </button>
      </form>

      {showDropdown && query.trim() && (
        <div className="autocomplete-dropdown">
          {results.length > 0 ? (
            <div className="autocomplete-list">
              {results.map((user) => (
                <button
                  key={user.id}
                  type="button"
                  className="autocomplete-item"
                  onClick={() => handleSelectUser(user)}
                >
                  <div className="item-avatar">{getInitials(user.username)}</div>
                  <div className="item-info">
                    <span className="item-username">{user.username}</span>
                    <span className="item-id">ID: #{user.id}</span>
                  </div>
                  <span className="item-action">Select</span>
                </button>
              ))}
            </div>
          ) : !loading ? (
            <div className="autocomplete-empty">
              <p>No existing users found matching "{query}".</p>
              <small>Click "Add Player" to submit as username directly.</small>
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
}
