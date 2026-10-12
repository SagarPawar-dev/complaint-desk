# Complaint Desk

College Complaint Management System — Full-Stack Web Application built for college grievance redressal.

## Tech Stack
- **Frontend**: HTML5, Vanilla CSS3 (Custom Design System), JavaScript (ES6)
- **Backend Architecture**: Java Servlets, JSP, JDBC, Apache Tomcat 10.1 (Jakarta EE)
- **Database**: PostgreSQL (Supabase)

## Folder Structure
```
CollegeComplaintSystemJava/
├── admin/                      # Administrator Workspace
│   ├── dashboard.html          # Global metrics & recent activity
│   ├── complaints.html         # Master complaints table with search & filters
│   ├── complaint-details.html  # Ticket details, timeline & admin control panel
│   ├── users.html              # User management & student complaint inspector
│   ├── notifications.html      # Alert center for new complaints
│   └── settings.html           # Administrator account & system settings
├── student/                    # Student Workspace
│   ├── dashboard.html          # Student overview & recent tickets
│   ├── report-complaint.html   # Grievance reporting form
│   ├── complaints.html         # Student complaint status tracking
│   ├── complaint-details.html  # Dedicated ticket details & activity timeline
│   ├── notifications.html      # Real-time alert stream for ticket updates
│   └── settings.html           # Student profile management
├── css/                        # Modular Stylesheets
│   ├── style.css               # Design tokens, reset, typography & buttons
│   ├── auth.css                # Authentication page styles
│   └── dashboard.css           # Sidebar, stat cards, tables, forms & timeline
├── js/                         # Application Logic
│   └── shared-data.js          # Shared mock data store & persistence
├── WEB-INF/
│   └── web.xml                 # Jakarta EE deployment descriptor
└── login.html                  # Role-based demo authentication portal
```

## Demo Credentials
- **Student**: `student@test.com` / `student123`
- **Admin**: `admin@test.com` / `admin123`
