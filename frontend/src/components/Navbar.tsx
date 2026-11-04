import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { SpadeIcon, LogOutIcon } from "./Icons";
import { getInitials } from "../utils/formatters";

export default function Navbar() {
  const { username, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <header className="navbar">
      <div className="navbar-container">
        <Link to="/dashboard" className="brand-logo">
          <div className="brand-icon-chip">
            <SpadeIcon size={18} className="brand-spade" />
          </div>
          <div className="brand-text">
            <span className="brand-name">STACKS</span>
            <span className="brand-sub">Poker Settlement</span>
          </div>
        </Link>

        <div className="navbar-right">
          {username && (
            <div className="user-pill">
              <div className="user-avatar">{getInitials(username)}</div>
              <span className="user-name">@{username}</span>
            </div>
          )}

          <button
            onClick={handleLogout}
            className="btn btn-logout"
            title="Sign out"
          >
            <LogOutIcon size={16} />
            <span className="logout-text">Sign Out</span>
          </button>
        </div>
      </div>
    </header>
  );
}
