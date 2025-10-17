# Script Fix Summary

**Date**: October 15, 2025
**Issue**: Maven/Java not found error when running `start.sh`
**Status**: ✅ **FIXED AND TESTED**

## Problem

When running `./start.sh`, users were getting:
```
Error: Maven is not installed or not in PATH
```

Even though Java and Maven were installed via SDKMAN.

## Root Cause

The `start.sh` script was checking for Java and Maven **before** initializing SDKMAN, so the commands weren't in the PATH yet.

## Solution

Fixed both `start.sh` and `dev.sh` scripts with two improvements:

### 1. Initialize SDKMAN First

Added SDKMAN initialization at the beginning of the script, before checking for dependencies:

```bash
# Initialize SDKMAN if available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    echo -e "${BLUE}Initializing SDKMAN...${NC}"
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# NOW check for Java and Maven
if ! command -v java &> /dev/null; then
    # Error message...
fi
```

### 2. Use Absolute Paths

Changed from relative paths to absolute paths so scripts work from any location:

```bash
# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use absolute paths
cd "$SCRIPT_DIR/api"
mvn spring-boot:run > "$SCRIPT_DIR/backend.log" 2>&1 &
```

## Files Updated

- ✅ `start.sh` - Fixed SDKMAN initialization and paths
- ✅ `dev.sh` - Fixed paths (already had SDKMAN init)

## Testing Results

### Test 1: start.sh Script ✅
```bash
./start.sh
```

**Output**:
```
========================================
  OSCAL CLI Web Interface
========================================

Initializing SDKMAN...
Starting backend server...
Waiting for backend to start...
✓ Backend is ready!
Starting frontend server...
Waiting for frontend to start...

========================================
  Servers are running!
========================================

Frontend: http://localhost:3000
Backend:  http://localhost:8080/api
```

**Result**: ✅ SUCCESS - Both servers started

### Test 2: Server Verification ✅

**Backend Health Check**:
```bash
curl http://localhost:8080/api/health
```
Response: `OSCAL CLI API is running`

**Frontend Check**:
```bash
curl -I http://localhost:3000
```
Response: `HTTP/1.1 200 OK`

**Result**: ✅ SUCCESS - Both servers responding

### Test 3: Validation Test ✅

**Test Document**: Minimal OSCAL catalog
**Result**: `Valid: True, Errors: 0`

**Result**: ✅ SUCCESS - Validation working

## How to Use Now

### Start the Application

```bash
cd front-end
./start.sh
```

The script will:
1. Initialize SDKMAN automatically
2. Check for Java, Maven, and Node.js
3. Start both servers
4. Show you the URLs and log file locations
5. Wait for you to press Ctrl+C to stop

### Quick Start (Background)

```bash
cd front-end
./dev.sh
```

Starts both servers in the background immediately.

### Stop the Application

```bash
cd front-end
./stop.sh
```

Or press `Ctrl+C` if you used `./start.sh`

## What Changed

| File | What Changed | Why |
|------|-------------|-----|
| `start.sh` | Added SDKMAN initialization at beginning | So Java/Maven are in PATH before checks |
| `start.sh` | Changed to absolute paths with `$SCRIPT_DIR` | Works from any directory |
| `start.sh` | Improved error messages | Shows how to install missing tools |
| `dev.sh` | Changed to absolute paths with `$SCRIPT_DIR` | Works from any directory |

## Benefits

✅ **No manual SDKMAN sourcing needed** - Scripts do it automatically
✅ **Works from any directory** - Use absolute paths
✅ **Better error messages** - Shows exactly how to fix issues
✅ **More reliable** - Checks happen after environment is set up

## Verification

Both scripts now work correctly:

- ✅ SDKMAN initialization happens automatically
- ✅ Java and Maven are found in PATH
- ✅ Backend starts successfully
- ✅ Frontend starts successfully
- ✅ Both servers communicate properly
- ✅ Validation endpoint works
- ✅ Error handling works

## Important Notes

### For First-Time Users

If Java and Maven are not installed at all, you'll see:
```
Error: Java is not installed or not in PATH
Please install Java 11+ using SDKMAN:
  curl -s "https://get.sdkman.io" | bash
  source ~/.sdkman/bin/sdkman-init.sh
  sdk install java 11.0.25-tem
```

Just follow those instructions once, then the scripts will work.

### For Existing Users

If you already have SDKMAN, Java, and Maven installed, the scripts now work immediately - no manual sourcing needed!

## Next Steps

The application is ready to use:

1. **Start**: `./start.sh` or `./dev.sh`
2. **Open**: http://localhost:3000
3. **Validate**: Upload or paste OSCAL documents
4. **Stop**: `./stop.sh` or press Ctrl+C

---

**Fix Status**: ✅ Complete
**Test Status**: ✅ All Passed
**Ready to Use**: ✅ Yes
