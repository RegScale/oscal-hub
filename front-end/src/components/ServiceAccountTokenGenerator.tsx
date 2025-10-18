'use client';

import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Key, Copy, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import { toast } from 'sonner';
import type { ServiceAccountTokenResponse } from '@/types/oscal';

export function ServiceAccountTokenGenerator() {
  const [tokenName, setTokenName] = useState('');
  const [expirationDays, setExpirationDays] = useState(30);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedToken, setGeneratedToken] = useState<ServiceAccountTokenResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const handleGenerate = async () => {
    if (!tokenName || expirationDays < 1) {
      toast.error('Please provide a token name and valid expiration days');
      return;
    }

    setIsGenerating(true);
    setGeneratedToken(null);

    try {
      const response = await apiClient.generateServiceAccountToken({
        tokenName,
        expirationDays,
      });

      setGeneratedToken(response);
      toast.success('Service account token generated successfully!');
    } catch (error: any) {
      console.error('Failed to generate token:', error);
      toast.error(error.message || 'Failed to generate service account token');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleCopy = async () => {
    if (!generatedToken) return;

    try {
      await navigator.clipboard.writeText(generatedToken.token);
      setCopied(true);
      toast.success('Token copied to clipboard!');

      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy token:', error);
      toast.error('Failed to copy token to clipboard');
    }
  };

  const handleReset = () => {
    setGeneratedToken(null);
    setTokenName('');
    setExpirationDays(30);
    setCopied(false);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Key className="h-5 w-5" />
          Service Account Tokens
        </CardTitle>
        <CardDescription>
          Generate JWT tokens for API access from external applications
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {!generatedToken ? (
          <>
            <Alert>
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription className="text-sm">
                Service account tokens provide full API access. Treat them like passwords.
                Tokens are not stored and cannot be retrieved after generation.
              </AlertDescription>
            </Alert>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="tokenName">Token Name</Label>
                <Input
                  id="tokenName"
                  type="text"
                  value={tokenName}
                  onChange={(e) => setTokenName(e.target.value)}
                  placeholder="e.g., CI/CD Pipeline, Mobile App, External Service"
                  disabled={isGenerating}
                />
                <p className="text-xs text-muted-foreground">
                  A descriptive name to help you identify this token
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="expirationDays">Expiration (days)</Label>
                <Input
                  id="expirationDays"
                  type="number"
                  min="1"
                  max="3650"
                  value={expirationDays}
                  onChange={(e) => setExpirationDays(parseInt(e.target.value) || 1)}
                  disabled={isGenerating}
                />
                <p className="text-xs text-muted-foreground">
                  Number of days until the token expires (1-3650 days)
                </p>
              </div>

              <Button
                onClick={handleGenerate}
                disabled={isGenerating || !tokenName}
                className="w-full"
                size="lg"
              >
                {isGenerating ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Generating Token...
                  </>
                ) : (
                  <>
                    <Key className="h-4 w-4 mr-2" />
                    Generate Service Account Token
                  </>
                )}
              </Button>
            </div>
          </>
        ) : (
          <>
            <Alert className="border-green-500/50 bg-green-500/10">
              <CheckCircle2 className="h-4 w-4 text-green-500" />
              <AlertDescription className="text-green-600">
                Service account token generated successfully! Copy it now - it won't be shown again.
              </AlertDescription>
            </Alert>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Token Name</Label>
                <p className="font-medium">{generatedToken.tokenName}</p>
              </div>

              <div className="space-y-2">
                <Label>Expires</Label>
                <p className="text-sm text-muted-foreground">
                  {new Date(generatedToken.expiresAt).toLocaleDateString(undefined, {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                  {' ('}
                  {generatedToken.expirationDays} days from now)
                </p>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label>Token</Label>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleCopy}
                  >
                    {copied ? (
                      <>
                        <CheckCircle2 className="h-4 w-4 mr-1 text-green-500" />
                        Copied!
                      </>
                    ) : (
                      <>
                        <Copy className="h-4 w-4 mr-1" />
                        Copy
                      </>
                    )}
                  </Button>
                </div>
                <div className="p-3 bg-muted rounded-lg font-mono text-sm break-all max-h-32 overflow-y-auto border">
                  {generatedToken.token}
                </div>
              </div>

              <Alert>
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription className="text-sm space-y-2">
                  <p className="font-semibold">Important Security Information:</p>
                  <ul className="list-disc list-inside space-y-1 text-xs">
                    <li>This token will not be displayed again</li>
                    <li>Store it securely (e.g., password manager, secrets vault)</li>
                    <li>Never commit it to version control</li>
                    <li>Use environment variables or secure configuration</li>
                    <li>Rotate tokens regularly for better security</li>
                  </ul>
                </AlertDescription>
              </Alert>

              <div className="flex gap-3">
                <Button onClick={handleCopy} className="flex-1" variant="default">
                  <Copy className="h-4 w-4 mr-2" />
                  Copy Token Again
                </Button>
                <Button onClick={handleReset} className="flex-1" variant="outline">
                  Create Another Token
                </Button>
              </div>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
