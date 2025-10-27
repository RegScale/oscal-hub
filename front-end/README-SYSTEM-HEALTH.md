# System Health Status Component

## Overview

The **System Health Status** component displays real-time health status of the OSCAL Tools backend services on the main landing page (before login). It uses red/green indicators to show at-a-glance system health.

## Features

- ✅ **Public Access** - Visible before login (no authentication required)
- ✅ **Real-Time Status** - Auto-refreshes every 30 seconds
- ✅ **Red/Green Indicators** - Color-coded status for each component
- ✅ **Multiple Components** - Tracks application, database, disk space, and cloud storage
- ✅ **Responsive Design** - Works on all screen sizes
- ✅ **Dark Mode Support** - Adapts to light/dark themes

## What It Shows

### Overall Application Status
- **Green (UP)** - Backend API is running and healthy
- **Red (DOWN)** - Backend API is not responding
- **Yellow (UNKNOWN)** - Unable to determine status

### Database (PostgreSQL)
- **Green (UP)** - Database connection is healthy
- **Red (DOWN)** - Cannot connect to database

### Disk Space
- **Green (UP)** - Sufficient disk space available
- **Red (DOWN)** - Low disk space (< 20% free)
- Shows free space in GB

### Cloud Storage (Azure Blob)
- **Green (UP)** - Storage is configured and accessible
- Shows whether using Azure Blob Storage or local fallback

## Technical Details

### Component Location
- **File**: `front-end/src/components/SystemHealth.tsx`
- **Used In**: `front-end/src/components/Hero.tsx` (landing page)

### API Endpoint
- **URL**: `/actuator/health`
- **Method**: GET
- **Auth**: Public (no authentication required)
- **Returns**: JSON health status

### Configuration

The `/actuator/health` endpoint is configured in:
```
back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java
```

Security configuration:
```java
.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
```

This allows public access to health checks without authentication.

### Auto-Refresh

The component automatically refreshes every 30 seconds to show up-to-date status. No user interaction required.

### Error Handling

If the backend is unreachable:
- Shows red "DOWN" status
- Displays error message: "Unable to connect to backend: [error]"
- Continues to retry every 30 seconds

## Example Health Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500107862016,
        "free": 250053931008,
        "threshold": 10485760,
        "exists": true
      }
    },
    "azureBlobStorage": {
      "status": "UP",
      "details": {
        "status": "configured",
        "storage": "azure_blob_storage",
        "message": "Azure Blob Storage is configured and ready"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

## Visual Design

### Status Badge Colors

**Green (UP)**:
- Background: Light green (`bg-green-100` / `bg-green-900/20`)
- Text: Dark green (`text-green-700` / `text-green-400`)
- Icon: Green checkmark circle

**Red (DOWN)**:
- Background: Light red (`bg-red-100` / `bg-red-900/20`)
- Text: Dark red (`text-red-700` / `text-red-400`)
- Icon: Red X circle

**Yellow (UNKNOWN)**:
- Background: Light yellow (`bg-yellow-100` / `bg-yellow-900/20`)
- Text: Dark yellow (`text-yellow-700` / `text-yellow-400`)
- Icon: Yellow alert circle

### Icons

- **Application**: Server icon
- **Database**: Database icon
- **Disk Space**: Hard drive icon
- **Cloud Storage**: Cloud icon
- **Activity**: Activity icon (component header)

## Usage Examples

### Viewing Health Status

1. Visit the OSCAL Hub landing page (before login)
2. Scroll down to the "System Health" section
3. View real-time status of all components

### Understanding Status

**All Green** - System is healthy
```
System Health: ✓ UP
  Application:    ✓ UP
  Database:       ✓ UP
  Disk Space:     ✓ UP  (250GB free)
  Cloud Storage:  ✓ UP  (azure_blob_storage)
```

**Database Down** - Database connection issue
```
System Health: ✗ DOWN
  Application:    ✓ UP
  Database:       ✗ DOWN
  Disk Space:     ✓ UP
  Cloud Storage:  ✓ UP
```

**Backend Down** - Backend not responding
```
System Health: ✗ DOWN
  ⚠ Unable to connect to backend: Connection refused
```

## Development

### Testing the Component

**Test with backend running**:
```bash
# Start backend
./start-all.sh

# Visit http://localhost:3000
# Should show all green statuses
```

**Test with backend stopped**:
```bash
# Stop backend
./stop-all.sh

# Visit http://localhost:3000
# Should show red DOWN status with error message
```

**Test auto-refresh**:
1. Start with backend stopped (shows red)
2. Start backend while watching the page
3. Within 30 seconds, status should turn green automatically

### Customization

**Change refresh interval** (SystemHealth.tsx):
```typescript
// Change from 30 seconds to 60 seconds
const interval = setInterval(fetchHealth, 60000);
```

**Add new health indicators**:
1. Update backend to expose new health component in `/actuator/health`
2. Update `HealthResponse` interface in SystemHealth.tsx
3. Add new UI element in the component

**Customize colors**:
Edit the `getStatusBadge()` function in SystemHealth.tsx to use different colors.

## Production Considerations

### Security

- ✅ Health endpoint is public (required for load balancer health checks)
- ✅ No sensitive data exposed (only UP/DOWN status)
- ✅ Details hidden by default (set `show-details: when-authorized` in production)

### Performance

- Minimal impact - single HTTP GET every 30 seconds
- Response size: ~1KB JSON
- Cached by browser for 30 seconds

### Monitoring

The health status displayed to users is the same data that Prometheus monitors:
- Grafana dashboard shows detailed health metrics
- Alerts fire if components are down
- Users can see basic status without login

## Troubleshooting

### "Unable to connect to backend"

**Problem**: Health component shows connection error

**Solutions**:
1. Check if backend is running: `docker-compose ps`
2. Verify backend URL in `.env.local`: `NEXT_PUBLIC_API_URL=http://localhost:8080/api`
3. Check backend logs: `docker-compose logs oscal-ux`
4. Verify health endpoint: `curl http://localhost:8080/actuator/health`

### Status shows "UNKNOWN"

**Problem**: Component status is yellow/unknown

**Causes**:
- Backend health check didn't return expected format
- New component added to backend but not frontend
- Health check threw an exception

**Solutions**:
1. Check backend logs for health check errors
2. Verify `/actuator/health` returns valid JSON
3. Update frontend interface to match backend response

### Auto-refresh not working

**Problem**: Status doesn't update automatically

**Solutions**:
1. Check browser console for JavaScript errors
2. Verify React useEffect is running
3. Clear browser cache and reload

## Related Documentation

- **Spring Boot Actuator**: `docs/MONITORING-GUIDE.md`
- **Grafana Dashboard**: `docs/MONITORING-DASHBOARD-SUMMARY.md`
- **Security Configuration**: `docs/ENVIRONMENT-BASED-SECURITY.md`

---

**Created**: October 26, 2025
**Component**: SystemHealth.tsx
**Visible**: Landing page (before login)
