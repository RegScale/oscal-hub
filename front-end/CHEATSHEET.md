# OSCAL CLI Web Interface - Quick Reference

## 🚀 Start the Application

```bash
cd front-end
./dev.sh              # Quick start (background)
# OR
./start.sh            # With logging and status
```

## 🌐 Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend UI** | http://localhost:3000 | Main web interface |
| **Backend API** | http://localhost:8080/api | REST API |
| **Health Check** | http://localhost:8080/api/health | Server status |

## 🛑 Stop Everything

```bash
cd front-end
./stop.sh             # Stop all servers
# OR
pkill -f 'spring-boot:run' && pkill -f 'next-server'
```

## 📝 Using the Validation Feature

1. Open http://localhost:3000
2. Click **"Validate"** in navigation
3. Upload file or paste content
4. Select **Model Type** (catalog, profile, etc.)
5. Select **Format** (JSON, XML, YAML)
6. Click **"Validate Document"**
7. View results

## 🔧 Common Commands

### Check if servers are running
```bash
curl http://localhost:8080/api/health  # Backend
curl -I http://localhost:3000          # Frontend
```

### View server logs (if using start.sh)
```bash
cd front-end
tail -f backend.log   # Backend logs
tail -f frontend.log  # Frontend logs
```

### Restart just the frontend
```bash
pkill -f 'next-server'
cd front-end/ui
npm run dev
```

### Restart just the backend
```bash
pkill -f 'spring-boot:run'
cd front-end/api
source ~/.sdkman/bin/sdkman-init.sh
mvn spring-boot:run
```

### Rebuild backend
```bash
cd front-end/api
mvn clean install
```

## 🐛 Quick Fixes

### "Java not found"
```bash
source ~/.sdkman/bin/sdkman-init.sh
```

### Port already in use
```bash
# Find process
lsof -i :8080  # Backend
lsof -i :3000  # Frontend

# Kill process
kill -9 <PID>
```

### Frontend can't reach backend
```bash
# 1. Verify backend is running
curl http://localhost:8080/api/health

# 2. Check config
cat front-end/ui/.env.local
# Should show: NEXT_PUBLIC_USE_MOCK=false

# 3. Restart frontend
pkill -f 'next-server'
cd front-end/ui && npm run dev
```

### Clear frontend cache
```bash
cd front-end/ui
rm -rf .next
npm run dev
```

## 📋 Test Commands

### Test backend validation
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{
    "content": "{\"catalog\":{\"uuid\":\"12345678-1234-1234-1234-123456789abc\",\"metadata\":{\"title\":\"Test\",\"last-modified\":\"2025-10-15T00:00:00Z\",\"version\":\"1.0\",\"oscal-version\":\"1.0.0\"}}}",
    "modelType": "catalog",
    "format": "JSON"
  }'
```

Expected response: `"valid": true`

## 📁 Project Structure

```
front-end/
├── api/           # Backend (Spring Boot)
├── ui/            # Frontend (Next.js)
├── start.sh       # Interactive start
├── dev.sh         # Quick start
├── stop.sh        # Stop all
├── QUICKSTART.md  # Full instructions
└── CHEATSHEET.md  # This file
```

## 🎯 Supported OSCAL Models

- `catalog` - Control catalogs
- `profile` - Control baselines
- `component-definition` - Component definitions
- `system-security-plan` - SSPs
- `assessment-plan` - Assessment plans
- `assessment-results` - Assessment results
- `plan-of-action-and-milestones` - POA&Ms

## 🎨 Supported Formats

- `JSON` - JSON format
- `XML` - XML format
- `YAML` - YAML format

## ⚙️ Configuration Files

| File | Purpose |
|------|---------|
| `api/src/main/resources/application.properties` | Backend config |
| `ui/.env.local` | Frontend config |
| `api/pom.xml` | Maven dependencies |
| `ui/package.json` | npm dependencies |

## 🔑 Environment Variables

Edit `front-end/ui/.env.local`:

```env
# Backend URL (default: http://localhost:8080/api)
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# Use mock data instead of real backend (default: false)
NEXT_PUBLIC_USE_MOCK=false
```

After changing: restart frontend to apply.

## 📚 Documentation

- **[QUICKSTART.md](QUICKSTART.md)** - Detailed setup and usage
- **[INTEGRATION-STATUS.md](INTEGRATION-STATUS.md)** - Integration details
- **[README.md](README.md)** - Architecture and design

## 💡 Pro Tips

1. **Add SDKMAN to shell**: Add to `~/.zshrc`:
   ```bash
   export SDKMAN_DIR="$HOME/.sdkman"
   [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```

2. **Create aliases** in `~/.zshrc`:
   ```bash
   alias oscal-start='cd ~/Documents/GitHub/oscal-cli/front-end && ./start.sh'
   alias oscal-stop='cd ~/Documents/GitHub/oscal-cli/front-end && ./stop.sh'
   ```

3. **Keep terminal open**: When using `start.sh`, keep the terminal open to see logs

4. **Browser DevTools**: Press `F12` in browser to see frontend errors

## ❓ Get Help

- Check server logs: `frontend.log` and `backend.log`
- Check browser console: Press F12
- Verify servers: `curl http://localhost:8080/api/health`
- Read [QUICKSTART.md](QUICKSTART.md) for troubleshooting

---

**Quick Start**: `cd front-end && ./dev.sh`
**Documentation**: [QUICKSTART.md](QUICKSTART.md)
**Status**: ✅ Validation feature fully operational
