'use client';

import { useState, useRef, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ShieldCheck, AlertTriangle, Loader2, Info, Pen, Eraser, Download } from 'lucide-react';

interface DigitalSignatureStepProps {
  authorizationId: number;
  authorizationName: string;
  onSignatureComplete: (result: SignatureResult) => void;
  onSkip: () => void;
}

interface SignatureResult {
  success: boolean;
  signerName?: string;
  signerEmail?: string;
  signerEdipi?: string;
  signatureTimestamp?: string;
  message?: string;
}

export function DigitalSignatureStep({
  authorizationId,
  authorizationName,
  onSignatureComplete,
  onSkip
}: DigitalSignatureStepProps) {
  const [signing, setSigning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Electronic signature state
  const [showElectronicSignature, setShowElectronicSignature] = useState(false);
  const [signerName, setSignerName] = useState('');
  const [signerTitle, setSignerTitle] = useState('');
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasSignature, setHasSignature] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [ctx, setCtx] = useState<CanvasRenderingContext2D | null>(null);

  // Initialize canvas
  useEffect(() => {
    if (canvasRef.current && showElectronicSignature) {
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');
      if (context) {
        context.strokeStyle = '#000000';
        context.lineWidth = 2;
        context.lineCap = 'round';
        context.lineJoin = 'round';
        setCtx(context);

        // Set canvas size
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width;
        canvas.height = rect.height;

        // Fill with white background
        context.fillStyle = '#ffffff';
        context.fillRect(0, 0, canvas.width, canvas.height);
      }
    }
  }, [showElectronicSignature]);

  // Canvas drawing functions
  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (!ctx || !canvasRef.current) return;

    setIsDrawing(true);
    const rect = canvasRef.current.getBoundingClientRect();
    const x = 'touches' in e ? e.touches[0].clientX - rect.left : e.nativeEvent.offsetX;
    const y = 'touches' in e ? e.touches[0].clientY - rect.top : e.nativeEvent.offsetY;

    ctx.beginPath();
    ctx.moveTo(x, y);
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (!isDrawing || !ctx || !canvasRef.current) return;

    e.preventDefault();
    const rect = canvasRef.current.getBoundingClientRect();
    const x = 'touches' in e ? e.touches[0].clientX - rect.left : e.nativeEvent.offsetX;
    const y = 'touches' in e ? e.touches[0].clientY - rect.top : e.nativeEvent.offsetY;

    ctx.lineTo(x, y);
    ctx.stroke();
    setHasSignature(true);
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const clearSignature = () => {
    if (!ctx || !canvasRef.current) return;

    const canvas = canvasRef.current;
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    setHasSignature(false);
  };

  const handleElectronicSign = async () => {
    if (!hasSignature || !signerName.trim()) {
      setError('Please provide your name and signature');
      return;
    }

    setSigning(true);
    setError(null);

    try {
      // Get signature as base64 image
      const signatureData = canvasRef.current?.toDataURL('image/png');

      if (!signatureData) {
        setError('Failed to capture signature image');
        return;
      }

      // Get stored JWT token
      const token = localStorage.getItem('token');
      if (!token) {
        setError('Authentication required. Please log in again.');
        return;
      }

      // Call API to save electronic signature
      const response = await fetch('/api/authorizations/sign-electronically', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          authorizationId: authorizationId,
          signerName: signerName,
          signerTitle: signerTitle || undefined,
          signatureImageData: signatureData
        })
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to sign' }));
        throw new Error(errorData.message || `Server error: ${response.status}`);
      }

      const result: SignatureResult = await response.json();

      if (result.success) {
        onSignatureComplete(result);
      } else {
        setError(result.message || 'Failed to save electronic signature');
      }
    } catch (err) {
      console.error('Electronic signing error:', err);
      setError(err instanceof Error ? err.message : 'Failed to save electronic signature');
    } finally {
      setSigning(false);
    }
  };

  const downloadSignature = () => {
    if (!canvasRef.current || !hasSignature) return;

    const link = document.createElement('a');
    link.download = `signature-${authorizationName.replace(/\s+/g, '-')}.png`;
    link.href = canvasRef.current.toDataURL('image/png');
    link.click();
  };

  const handleSign = async () => {
    setSigning(true);
    setError(null);

    try {
      // Call the signing endpoint
      // This will trigger browser's certificate selection dialog
      const response = await fetch('/api/authorizations/sign-with-cert', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // Important: include credentials for client cert
        body: JSON.stringify({
          authorizationId
        })
      });

      if (response.ok) {
        const result: SignatureResult = await response.json();

        if (result.success) {
          onSignatureComplete(result);
        } else {
          setError(result.message || 'Signing failed');
        }
      } else {
        let errorMessage = `Signing failed with status ${response.status}`;

        try {
          const errorData = await response.json();
          if (errorData.message) {
            errorMessage = errorData.message;
          }
        } catch {
          // If response is not JSON, use default error message
        }

        if (response.status === 401) {
          errorMessage = 'No CAC/PIV certificate provided. Please ensure your card is inserted and ActivClient is running.';
        }

        setError(errorMessage);
      }
    } catch (err) {
      console.error('Signing error:', err);
      setError('Failed to sign authorization. Please ensure your CAC/PIV card is inserted and ActivClient is running.');
    } finally {
      setSigning(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Signature method selection */}
      <div className="flex gap-4">
        <Button
          variant={!showElectronicSignature ? 'default' : 'outline'}
          onClick={() => setShowElectronicSignature(false)}
          className="flex-1"
        >
          <ShieldCheck className="h-5 w-5 mr-2" />
          CAC/PIV Signature
        </Button>
        <Button
          variant={showElectronicSignature ? 'default' : 'outline'}
          onClick={() => setShowElectronicSignature(true)}
          className="flex-1"
        >
          <Pen className="h-5 w-5 mr-2" />
          Electronic Signature
        </Button>
      </div>

      {/* Electronic Signature Section */}
      {showElectronicSignature ? (
        <Card className="p-6">
          <div className="flex items-start gap-4 mb-6">
            <Pen className="h-8 w-8 text-purple-500 flex-shrink-0" />
            <div>
              <h3 className="text-xl font-semibold mb-2">Electronic Signature</h3>
              <p className="text-gray-600 dark:text-gray-400">
                Sign the authorization &quot;{authorizationName}&quot; by drawing your signature with your mouse or touchscreen.
              </p>
            </div>
          </div>

          {/* Signer Information */}
          <div className="space-y-4 mb-6">
            <div>
              <Label htmlFor="signer-name">Full Name *</Label>
              <Input
                id="signer-name"
                type="text"
                placeholder="Enter your full name"
                value={signerName}
                onChange={(e) => setSignerName(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="signer-title">Title/Position (Optional)</Label>
              <Input
                id="signer-title"
                type="text"
                placeholder="Enter your title or position"
                value={signerTitle}
                onChange={(e) => setSignerTitle(e.target.value)}
              />
            </div>
          </div>

          {/* Signature Canvas */}
          <div className="mb-6">
            <Label className="mb-2 block">Draw Your Signature *</Label>
            <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden bg-white">
              <canvas
                ref={canvasRef}
                onMouseDown={startDrawing}
                onMouseMove={draw}
                onMouseUp={stopDrawing}
                onMouseLeave={stopDrawing}
                onTouchStart={startDrawing}
                onTouchMove={draw}
                onTouchEnd={stopDrawing}
                className="w-full h-48 cursor-crosshair touch-none"
                style={{ touchAction: 'none' }}
              />
            </div>
            <div className="flex gap-2 mt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={clearSignature}
                disabled={!hasSignature}
              >
                <Eraser className="h-4 w-4 mr-2" />
                Clear
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={downloadSignature}
                disabled={!hasSignature}
              >
                <Download className="h-4 w-4 mr-2" />
                Download
              </Button>
            </div>
          </div>

          {/* Error display */}
          {error && (
            <Alert className="mb-6 bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
              <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <AlertDescription>
                <p className="font-semibold text-red-900 dark:text-red-100">Signing Failed</p>
                <p className="text-sm mt-1 text-red-800 dark:text-red-200">{error}</p>
              </AlertDescription>
            </Alert>
          )}

          {/* Action buttons */}
          <div className="flex gap-3">
            <Button
              onClick={handleElectronicSign}
              disabled={signing || !hasSignature || !signerName.trim()}
              className="flex-1"
              size="lg"
            >
              {signing ? (
                <>
                  <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                  Saving signature...
                </>
              ) : (
                <>
                  <Pen className="h-5 w-5 mr-2" />
                  Sign Electronically
                </>
              )}
            </Button>

            <Button
              variant="outline"
              onClick={onSkip}
              disabled={signing}
              size="lg"
            >
              Skip Signature
            </Button>
          </div>

          {/* Info about electronic signature */}
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-3 text-center">
            Electronic signatures provide a visual record of approval. For cryptographic proof and federal compliance,
            use CAC/PIV signature instead.
          </p>
        </Card>
      ) : (
        /* CAC/PIV Signature Section */
        <Card className="p-6">
          <div className="flex items-start gap-4 mb-6">
            <ShieldCheck className="h-8 w-8 text-blue-500 flex-shrink-0" />
            <div>
              <h3 className="text-xl font-semibold mb-2">Digital Signature with CAC/PIV</h3>
              <p className="text-gray-600 dark:text-gray-400">
                Sign the authorization &quot;{authorizationName}&quot; using your CAC or PIV card to provide
                cryptographic proof of authorization.
              </p>
            </div>
          </div>

          {/* Instructions */}
          <Alert className="mb-6 bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
            <Info className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            <AlertDescription>
              <div className="space-y-2">
                <p className="font-semibold text-blue-900 dark:text-blue-100">Before signing:</p>
                <ul className="list-disc ml-6 space-y-1 text-blue-800 dark:text-blue-200">
                  <li>Ensure your CAC/PIV card is inserted into the card reader</li>
                  <li>Verify ActivClient is running (look for system tray icon)</li>
                  <li>Have your PIN ready</li>
                </ul>
              </div>
            </AlertDescription>
          </Alert>

          {/* What happens when you sign */}
          <div className="mb-6 p-4 bg-slate-50 dark:bg-slate-800/50 rounded-lg border border-slate-200 dark:border-slate-700">
            <h4 className="font-semibold mb-2 text-slate-900 dark:text-slate-100">What happens when you click &quot;Sign&quot;:</h4>
            <ol className="list-decimal ml-6 space-y-1 text-sm text-slate-700 dark:text-slate-300">
              <li>Your browser will show a certificate selection dialog</li>
              <li>Select your CAC/PIV signature certificate</li>
              <li>ActivClient will prompt for your PIN</li>
              <li>Enter your PIN to complete the signature</li>
              <li>The authorization will be digitally signed</li>
            </ol>
          </div>

          {/* Error display */}
          {error && (
            <Alert className="mb-6 bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
              <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <AlertDescription>
                <p className="font-semibold text-red-900 dark:text-red-100">Signing Failed</p>
                <p className="text-sm mt-1 text-red-800 dark:text-red-200">{error}</p>
                <div className="mt-3">
                  <p className="text-sm font-semibold text-red-800 dark:text-red-200">
                    Common solutions:
                  </p>
                  <ul className="list-disc ml-6 text-sm mt-1 text-red-700 dark:text-red-300">
                    <li>Ensure your card is fully inserted</li>
                    <li>Restart ActivClient</li>
                    <li>Check that your certificate is not expired</li>
                    <li>Verify your PIN is correct</li>
                  </ul>
                </div>
              </AlertDescription>
            </Alert>
          )}

          {/* Action buttons */}
          <div className="flex gap-3">
          <Button
            onClick={handleSign}
            disabled={signing}
            className="flex-1"
            size="lg"
          >
            {signing ? (
              <>
                <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                Waiting for signature...
              </>
            ) : (
              <>
                <ShieldCheck className="h-5 w-5 mr-2" />
                Sign with CAC/PIV
              </>
            )}
          </Button>

          <Button
            variant="outline"
            onClick={onSkip}
            disabled={signing}
            size="lg"
          >
            Skip Signature
          </Button>
        </div>

        {/* Info about skipping */}
        <p className="text-xs text-gray-500 dark:text-gray-400 mt-3 text-center">
          Skipping the digital signature will create the authorization without cryptographic proof.
          Digital signatures provide non-repudiation and compliance with federal PKI requirements.
        </p>
      </Card>
      )}

      {/* Technical details (collapsible) - only for CAC/PIV */}
      {!showElectronicSignature && (
        <details className="text-sm">
          <summary className="cursor-pointer text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200">
            Technical Details
          </summary>
          <div className="mt-2 p-4 bg-gray-50 dark:bg-gray-900 rounded border border-gray-200 dark:border-gray-700 text-xs space-y-2">
            <p><strong>Signature Method:</strong> TLS Client Certificate Authentication</p>
            <p><strong>Certificate Type:</strong> CAC/PIV Signature Certificate</p>
            <p><strong>Middleware:</strong> ActivClient (HID Global)</p>
            <p><strong>Compliance:</strong> FIPS 201, NIST SP 800-53, DoD PKI</p>
            <p><strong>Authorization ID:</strong> {authorizationId}</p>
            <p><strong>Security:</strong> Private keys never leave the smart card</p>
          </div>
        </details>
      )}
    </div>
  );
}
