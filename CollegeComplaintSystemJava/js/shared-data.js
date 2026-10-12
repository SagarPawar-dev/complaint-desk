/**
 * ==============================================================================
 * College Complaint Management System - Shared Mock Data Module (Phase 2)
 * ==============================================================================
 * Manages persistent local data via browser localStorage across all HTML pages.
 * Simulates real database operations:
 *   - Auto-incrementing complaint IDs (CMP-00001, CMP-00002, ...)
 *   - Chronological Activity Timelines (submission, status updates, admin remarks)
 *   - Cross-role Notification triggers (student <-> admin)
 * ==============================================================================
 */

const STORAGE_KEYS = {
  COMPLAINTS: "ccs_complaints_data",
  NOTIFICATIONS: "ccs_notifications_data"
};

// Initial default seed complaints matching the original screenshots
const DEFAULT_COMPLAINTS = [
  {
    id: "CMP-00002",
    title: "fan not working",
    category: "Electrical",
    priority: "Medium",
    location: "Library 1st Floor, Reading Hall (Fan #3)",
    description: "Ceiling fan #3 makes a loud rattling noise and stopped spinning completely since yesterday.",
    status: "PENDING",
    submittedBy: "student@test.com",
    studentName: "Om Sharma",
    studentDept: "Computer Science & Engineering",
    submittedAt: "05/10/2026 10:30 AM",
    timeline: [
      {
        action: "Complaint Submitted",
        by: "Om Sharma (Student)",
        time: "05/10/2026 10:30 AM",
        note: "Initial grievance submitted with Medium priority.",
        status: "PENDING"
      }
    ]
  },
  {
    id: "CMP-00001",
    title: "Classroom Projector Not Working",
    category: "IT / Equipment",
    priority: "High",
    location: "Block B, Room 204",
    description: "Projector lamp is not turning on and HDMI cable socket is damaged.",
    status: "CLOSED",
    submittedBy: "student@test.com",
    studentName: "Om Sharma",
    studentDept: "Computer Science & Engineering",
    submittedAt: "03/10/2026 09:15 AM",
    timeline: [
      {
        action: "Complaint Submitted",
        by: "Om Sharma (Student)",
        time: "03/10/2026 09:15 AM",
        note: "Initial report of projector failure.",
        status: "PENDING"
      },
      {
        action: "Status Updated to IN PROGRESS",
        by: "System Administrator",
        time: "03/10/2026 11:45 AM",
        note: "Technician assigned to inspect the projector in Room 204.",
        status: "IN PROGRESS"
      },
      {
        action: "Complaint Resolved & Closed",
        by: "System Administrator",
        time: "03/10/2026 04:15 PM",
        note: "Replaced lamp module and renewed HDMI wall socket. Verified functional.",
        status: "CLOSED"
      }
    ]
  }
];

// Initial default notifications
const DEFAULT_NOTIFICATIONS = [
  {
    id: "NOTIF-001",
    complaintId: "CMP-00001",
    recipientEmail: "student@test.com",
    recipientRole: "STUDENT",
    title: "Complaint Resolved (CMP-00001)",
    message: "Admin closed 'Classroom Projector Not Working'. Remark: Replaced lamp module and renewed HDMI socket.",
    time: "03/10/2026 04:15 PM",
    type: "RESOLVED",
    read: false
  },
  {
    id: "NOTIF-002",
    complaintId: "CMP-00002",
    recipientEmail: "admin@test.com",
    recipientRole: "ADMIN",
    title: "New Complaint Submitted (CMP-00002)",
    message: "Om Sharma submitted 'fan not working' in Library 1st Floor.",
    time: "05/10/2026 10:30 AM",
    type: "NEW_SUBMISSION",
    read: false
  },
  {
    id: "NOTIF-003",
    complaintId: "CMP-00002",
    recipientEmail: "student@test.com",
    recipientRole: "STUDENT",
    title: "Complaint Under Review (CMP-00002)",
    message: "Admin started reviewing 'fan not working' in Library 1st Floor.",
    time: "05/10/2026 10:35 AM",
    type: "PENDING",
    read: false
  }
];

// Initialize localStorage on first run
function initStorage() {
  if (!localStorage.getItem(STORAGE_KEYS.COMPLAINTS)) {
    localStorage.setItem(STORAGE_KEYS.COMPLAINTS, JSON.stringify(DEFAULT_COMPLAINTS));
  } else {
    try {
      const data = JSON.parse(localStorage.getItem(STORAGE_KEYS.COMPLAINTS));
      const cleaned = data.filter(c => c.submittedBy !== "priya@test.com");
      localStorage.setItem(STORAGE_KEYS.COMPLAINTS, JSON.stringify(cleaned));
    } catch (e) {}
  }
  if (!localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS)) {
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(DEFAULT_NOTIFICATIONS));
  } else {
    try {
      const notifs = JSON.parse(localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS));
      let changed = false;
      notifs.forEach(n => {
        if (typeof n.read !== "boolean") {
          n.read = false;
          changed = true;
        }
      });
      if (changed) {
        localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(notifs));
      }
    } catch (e) {}
  }
}

initStorage();

// Shared Data API
const ComplaintStore = {
  getAllComplaints: function () {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.COMPLAINTS);
      return data ? JSON.parse(data) : DEFAULT_COMPLAINTS;
    } catch (e) {
      return DEFAULT_COMPLAINTS;
    }
  },

  getStudentComplaints: function (studentEmail) {
    const all = this.getAllComplaints();
    if (!studentEmail) return all;
    return all.filter(c => c.submittedBy === studentEmail);
  },

  getComplaintById: function (id) {
    const all = this.getAllComplaints();
    return all.find(c => c.id === id) || null;
  },

  getUsers: function () {
    return [
      { id: "USR-101", name: "Om Sharma", email: "student@test.com", dept: "Computer Science & Engineering", role: "STUDENT", status: "ACTIVE" },
      { id: "USR-001", name: "System Administrator", email: "admin@test.com", dept: "College Administration", role: "ADMIN", status: "ACTIVE" }
    ];
  },

  createComplaint: function (data) {
    const all = this.getAllComplaints();
    
    // Generate next ID
    const nextNum = all.length + 1;
    const padded = String(nextNum).padStart(5, '0');
    const newId = `CMP-${padded}`;

    const now = new Date();
    const formattedDate = now.toLocaleDateString('en-GB') + ' ' + now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const newComplaint = {
      id: newId,
      title: data.title,
      category: data.category,
      priority: data.priority || "Medium",
      location: data.location || "Main Campus",
      description: data.description,
      status: "PENDING",
      submittedBy: data.submittedBy || "student@test.com",
      studentName: data.studentName || "Om Sharma",
      studentDept: data.studentDept || "Computer Science & Engineering",
      submittedAt: formattedDate,
      timeline: [
        {
          action: "Complaint Submitted",
          by: `${data.studentName || "Student"} (Student)`,
          time: formattedDate,
          note: "Initial grievance submitted with " + (data.priority || "Medium") + " priority.",
          status: "PENDING"
        }
      ]
    };

    // Prepend new complaint so it appears at top
    all.unshift(newComplaint);
    localStorage.setItem(STORAGE_KEYS.COMPLAINTS, JSON.stringify(all));

    // Create notification for ADMIN
    this.addNotification({
      complaintId: newId,
      recipientEmail: "admin@test.com",
      recipientRole: "ADMIN",
      title: `New Complaint: ${newComplaint.title} (${newId})`,
      message: `${newComplaint.studentName} filed a new complaint under ${newComplaint.category}.`,
      time: formattedDate,
      type: "NEW_SUBMISSION"
    });

    return newComplaint;
  },

  updateComplaintStatus: function (id, newStatus, adminRemark) {
    const all = this.getAllComplaints();
    const index = all.findIndex(c => c.id === id);
    if (index === -1) return null;

    const complaint = all[index];
    const oldStatus = complaint.status;
    complaint.status = newStatus;

    const now = new Date();
    const formattedDate = now.toLocaleDateString('en-GB') + ' ' + now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Add timeline entry
    complaint.timeline.push({
      action: `Status Changed from ${oldStatus} to ${newStatus}`,
      by: "System Administrator",
      time: formattedDate,
      note: adminRemark ? `Admin Remark: "${adminRemark}"` : "Status was updated by administrator.",
      status: newStatus
    });

    all[index] = complaint;
    localStorage.setItem(STORAGE_KEYS.COMPLAINTS, JSON.stringify(all));

    // Notify the student
    this.addNotification({
      complaintId: id,
      recipientEmail: complaint.submittedBy,
      recipientRole: "STUDENT",
      title: `Complaint Status Updated (${id})`,
      message: `Your complaint '${complaint.title}' is now ${newStatus}. ${adminRemark ? `Admin remarked: "${adminRemark}"` : ""}`,
      time: formattedDate,
      type: newStatus
    });

    return complaint;
  },

  addAdminResponse: function (id, responseText) {
    const all = this.getAllComplaints();
    const index = all.findIndex(c => c.id === id);
    if (index === -1) return null;

    const complaint = all[index];
    const now = new Date();
    const formattedDate = now.toLocaleDateString('en-GB') + ' ' + now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Add timeline entry for response
    complaint.timeline.push({
      action: "Admin Response",
      by: "System Administrator",
      time: formattedDate,
      note: responseText,
      status: complaint.status
    });

    all[index] = complaint;
    localStorage.setItem(STORAGE_KEYS.COMPLAINTS, JSON.stringify(all));

    // Notify the student
    this.addNotification({
      complaintId: id,
      recipientEmail: complaint.submittedBy,
      recipientRole: "STUDENT",
      title: `New Response on Complaint (${id})`,
      message: `Admin added a response: "${responseText.substring(0, 80)}${responseText.length > 80 ? '...' : ''}"`,
      time: formattedDate,
      type: "RESPONSE"
    });

    return complaint;
  },

  getNotifications: function (role, email) {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      const all = data ? JSON.parse(data) : DEFAULT_NOTIFICATIONS;
      if (role === "ADMIN") {
        return all.filter(n => n.recipientRole === "ADMIN");
      }
      return all.filter(n => n.recipientRole === "STUDENT" && (!email || n.recipientEmail === email));
    } catch (e) {
      return [];
    }
  },

  getUnreadCount: function (role, email) {
    const list = this.getNotifications(role, email);
    return list.filter(n => !n.read).length;
  },

  markNotificationAsRead: function (id) {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      const all = data ? JSON.parse(data) : DEFAULT_NOTIFICATIONS;
      const index = all.findIndex(n => n.id === id);
      if (index !== -1) {
        all[index].read = true;
        localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(all));
        return all[index];
      }
    } catch (e) {}
    return null;
  },

  markAllNotificationsAsRead: function (role, email) {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      const all = data ? JSON.parse(data) : DEFAULT_NOTIFICATIONS;
      all.forEach(n => {
        if (role === "ADMIN" && n.recipientRole === "ADMIN") {
          n.read = true;
        } else if (role === "STUDENT" && n.recipientRole === "STUDENT" && (!email || n.recipientEmail === email)) {
          n.read = true;
        }
      });
      localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(all));
    } catch (e) {}
  },

  updateBellBadge: function (role, email) {
    const count = this.getUnreadCount(role, email);
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

  addNotification: function (notif) {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      const all = data ? JSON.parse(data) : [];
      notif.id = "NOTIF-" + Date.now();
      notif.read = false;
      all.unshift(notif);
      localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(all));
    } catch (e) {}
  },

  getStats: function (studentEmail) {
    const list = studentEmail ? this.getStudentComplaints(studentEmail) : this.getAllComplaints();
    const total = list.length;
    const pending = list.filter(c => c.status === "PENDING" || c.status === "SUBMITTED").length;
    const progress = list.filter(c => c.status === "IN PROGRESS" || c.status === "IN_PROGRESS").length;
    const resolved = list.filter(c => c.status === "RESOLVED" || c.status === "CLOSED").length;
    return { total, pending, progress, resolved };
  }
};

// Auto-initialize Bell Badge across all pages
if (typeof document !== "undefined") {
  document.addEventListener("DOMContentLoaded", function () {
    const path = (window.location.pathname || "").toLowerCase();
    const isAdmin = path.includes("/admin/");
    let role = isAdmin ? "ADMIN" : "STUDENT";
    let email = isAdmin ? "admin@test.com" : "student@test.com";
    try {
      const demoUser = JSON.parse(sessionStorage.getItem("demoUser"));
      if (demoUser && demoUser.email) {
        email = demoUser.email;
        if (demoUser.role) role = demoUser.role;
      }
    } catch (e) {}

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

    ComplaintStore.updateBellBadge(role, email);
  });
}
