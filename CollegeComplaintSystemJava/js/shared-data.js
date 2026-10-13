/**
 * ==============================================================================
 * College Complaint Management System - Backend Integration Module
 * ==============================================================================
 * Connects frontend interfaces directly to Jakarta Servlets and Supabase PostgreSQL:
 *   - /api/login and /api/logout
 *   - /api/complaints, /api/complaints/status, /api/complaints/response
 *   - /api/stats
 *   - /api/notifications
 *   - /api/users
 * ==============================================================================
 */

const ComplaintStore = {
  /**
   * Helper to resolve relative API path depending on current subfolder (/student/ or /admin/)
   */
  getApiUrl: function (endpoint) {
    const clean = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
    const path = (window.location.pathname || "").toLowerCase();
    if (path.includes("/student/") || path.includes("/admin/")) {
      return "../" + clean;
    }
    return clean;
  },

  /**
   * Retrieves all institution complaints from PostgreSQL (Admin workspace)
   */
  getAllComplaints: async function () {
    try {
      const res = await fetch(this.getApiUrl("api/complaints"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) {
        throw new Error("Failed to load complaints (HTTP " + res.status + ")");
      }
      return await res.json();
    } catch (err) {
      console.error("[ComplaintStore] getAllComplaints error:", err);
      return [];
    }
  },

  /**
   * Retrieves complaints for a specific student (or current student session)
   */
  getStudentComplaints: async function (studentEmail) {
    try {
      const url = studentEmail
        ? this.getApiUrl("api/complaints?studentEmail=" + encodeURIComponent(studentEmail))
        : this.getApiUrl("api/complaints");
      const res = await fetch(url, {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) {
        throw new Error("Failed to load student complaints (HTTP " + res.status + ")");
      }
      return await res.json();
    } catch (err) {
      console.error("[ComplaintStore] getStudentComplaints error:", err);
      return [];
    }
  },

  /**
   * Retrieves a single complaint by ticket ID including full timeline history
   */
  getComplaintById: async function (id) {
    if (!id) return null;
    try {
      const res = await fetch(this.getApiUrl("api/complaints?id=" + encodeURIComponent(id)), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) {
        if (res.status === 404) return null;
        throw new Error("Complaint " + id + " error (HTTP " + res.status + ")");
      }
      return await res.json();
    } catch (err) {
      console.error("[ComplaintStore] getComplaintById error:", err);
      return null;
    }
  },

  /**
   * Submits a new grievance to the database
   */
  createComplaint: async function (data) {
    try {
      const res = await fetch(this.getApiUrl("api/complaints"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        credentials: "same-origin",
        body: new URLSearchParams({
          title: data.title || "",
          category: data.category || "",
          priority: data.priority || "Medium",
          location: data.location || "",
          description: data.description || ""
        })
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Failed to create complaint (HTTP " + res.status + ")");
      }
      return json;
    } catch (err) {
      console.error("[ComplaintStore] createComplaint error:", err);
      throw err;
    }
  },

  /**
   * Updates complaint status with an administrative remark
   */
  updateComplaintStatus: async function (id, newStatus, adminRemark) {
    try {
      const res = await fetch(this.getApiUrl("api/complaints/status"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        credentials: "same-origin",
        body: new URLSearchParams({
          id: id,
          status: newStatus,
          remark: adminRemark || ""
        })
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Failed to update complaint status");
      }
      return json;
    } catch (err) {
      console.error("[ComplaintStore] updateComplaintStatus error:", err);
      throw err;
    }
  },

  /**
   * Posts an administrative response to the complaint timeline
   */
  addAdminResponse: async function (id, responseText) {
    try {
      const res = await fetch(this.getApiUrl("api/complaints/response"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        credentials: "same-origin",
        body: new URLSearchParams({
          id: id,
          response: responseText
        })
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Failed to post response");
      }
      return json;
    } catch (err) {
      console.error("[ComplaintStore] addAdminResponse error:", err);
      throw err;
    }
  },

  /**
   * Permanently deletes a complaint and dependent records (Admin only)
   */
  deleteComplaint: async function (id) {
    if (!id) throw new Error("Complaint ID is required");
    try {
      const res = await fetch(this.getApiUrl("api/complaints/delete"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        credentials: "same-origin",
        body: new URLSearchParams({ id: id })
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Failed to delete complaint (HTTP " + res.status + ")");
      }
      return json;
    } catch (err) {
      console.error("[ComplaintStore] deleteComplaint error:", err);
      throw err;
    }
  },

  /**
   * Fetches real-time statistics from PostgreSQL
   */
  getStats: async function (studentEmail) {
    try {
      const res = await fetch(this.getApiUrl("api/stats"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) {
        throw new Error("HTTP " + res.status);
      }
      return await res.json();
    } catch (err) {
      console.error("[ComplaintStore] getStats error:", err);
      return { total: 0, pending: 0, progress: 0, resolved: 0 };
    }
  },

  /**
   * Fetches alerts and notifications for the current authenticated user
   */
  getNotifications: async function (role, email) {
    try {
      const res = await fetch(this.getApiUrl("api/notifications"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) {
        throw new Error("HTTP " + res.status);
      }
      return await res.json();
    } catch (err) {
      console.error("[ComplaintStore] getNotifications error:", err);
      return [];
    }
  },

  /**
   * Gets unread alert count for current user
   */
  getUnreadCount: async function (role, email) {
    try {
      const res = await fetch(this.getApiUrl("api/notifications?action=unread-count"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) return 0;
      const data = await res.json();
      return typeof data.count === "number" ? data.count : 0;
    } catch (err) {
      return 0;
    }
  },

  /**
   * Marks a single notification as read
   */
  markNotificationAsRead: async function (id) {
    try {
      const res = await fetch(this.getApiUrl("api/notifications"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        credentials: "same-origin",
        body: new URLSearchParams({ id: id })
      });
      return res.ok;
    } catch (err) {
      return false;
    }
  },

  /**
   * Marks all notifications as read for current user
   */
  markAllNotificationsAsRead: async function (role, email) {
    try {
      const res = await fetch(this.getApiUrl("api/notifications?action=read-all"), {
        method: "POST",
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      return res.ok;
    } catch (err) {
      return false;
    }
  },

  /**
   * Fetches institutional users from PostgreSQL (Admin User Management)
   */
  getUsers: async function () {
    try {
      const res = await fetch(this.getApiUrl("api/users"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (!res.ok) {
        throw new Error("HTTP " + res.status);
      }
      return await res.json();
    } catch (err) {
      console.error("[ComplaintStore] getUsers error:", err);
      return [];
    }
  },

  /**
   * Retrieves profile details for the currently logged in user
   */
  getCurrentUser: async function () {
    try {
      const res = await fetch(this.getApiUrl("api/users/me"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (res.ok) {
        const u = await res.json();
        if (u && u.email) return u;
      }
    } catch (err) {}
    try {
      return JSON.parse(sessionStorage.getItem("demoUser")) || null;
    } catch (e) {
      return null;
    }
  },

  /**
   * Updates authenticated user profile details (full name and department)
   */
  updateProfile: async function (fullName, department) {
    if (!fullName || !fullName.trim()) throw new Error("Name cannot be empty");
    try {
      const res = await fetch(this.getApiUrl("api/users/profile"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Accept": "application/json"
        },
        credentials: "same-origin",
        body: new URLSearchParams({
          fullName: fullName.trim(),
          department: (department || "").trim()
        })
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Failed to update profile (HTTP " + res.status + ")");
      }

      // Keep client-side cache & UI synchronized
      const updatedName = json.user && json.user.fullName ? json.user.fullName : fullName.trim();
      try {
        const stored = JSON.parse(sessionStorage.getItem("demoUser")) || {};
        stored.name = updatedName;
        if (json.user && json.user.department) stored.department = json.user.department;
        sessionStorage.setItem("demoUser", JSON.stringify(stored));
      } catch (e) {}

      // Refresh any name elements across the DOM
      const nameDisplays = document.querySelectorAll("#userNameDisplay, .user-name");
      nameDisplays.forEach(el => {
        el.textContent = updatedName;
      });

      return json;
    } catch (err) {
      console.error("[ComplaintStore] updateProfile error:", err);
      throw err;
    }
  },

  /**
   * Updates top header notification bell badge
   */
  updateBellBadge: async function (role, email) {
    const count = await this.getUnreadCount(role, email);
    const badges = document.querySelectorAll(".notification-badge");
    badges.forEach(badge => {
      if (count > 0) {
        badge.textContent = count;
        badge.style.display = "flex";
      } else {
        badge.textContent = "0";
        badge.style.display = "none";
      }
    });
  },

  /**
   * Client-side session verification.
   * Validates server-side session with /api/login and redirects unauthenticated users.
   */
  verifySession: async function () {
    const path = (window.location.pathname || "").toLowerCase();
    const isAdmin = path.includes("/admin/");
    const isStudent = path.includes("/student/");
    if (!isAdmin && !isStudent) return;

    try {
      const res = await fetch(this.getApiUrl("api/login"), {
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
      if (res.ok) {
        const data = await res.json();
        if (!data || !data.authenticated) {
          window.location.replace("../login.html");
          return;
        }
        if (isAdmin && data.role !== "ADMIN") {
          window.location.replace("../student/dashboard.html");
          return;
        }
        if (isStudent && data.role !== "STUDENT") {
          window.location.replace("../admin/dashboard.html");
          return;
        }

        // Keep local profile display fresh
        sessionStorage.setItem("demoUser", JSON.stringify({
          id: data.id,
          email: data.email,
          role: data.role,
          name: data.name,
          department: data.department
        }));

        // Dynamically update user name on top header
        const nameDisplays = document.querySelectorAll("#userNameDisplay, .user-name");
        nameDisplays.forEach(el => {
          if (data.name) el.textContent = data.name;
        });

      } else {
        window.location.replace("../login.html");
      }
    } catch (err) {
      console.warn("[ComplaintStore] verifySession failed:", err);
    }
  },

  /**
   * Terminates server-side session and redirects to login
   */
  logout: async function () {
    try {
      sessionStorage.clear();
      await fetch(this.getApiUrl("api/logout"), {
        method: "POST",
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });
    } catch (err) {}
    window.location.href = "../login.html";
  }
};

// Auto-initialize Bell Badge and Session Verification across all pages
if (typeof document !== "undefined") {
  document.addEventListener("DOMContentLoaded", function () {
    ComplaintStore.verifySession();

    // Ensure all bell buttons have a badge element attached
    const bellBtns = document.querySelectorAll(".notification-bell-btn");
    bellBtns.forEach(btn => {
      if (!btn.querySelector(".notification-badge")) {
        const badge = document.createElement("span");
        badge.className = "notification-badge";
        badge.style.display = "none";
        btn.appendChild(badge);
      }
    });

    ComplaintStore.updateBellBadge();
  });
}
