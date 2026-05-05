'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Search, ShieldCheck, AlertCircle, AlertTriangle, Info, Filter, Settings, ChevronRight } from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import ProtectedRoute from '@/components/ProtectedRoute';
import { HelpButton } from '@/components/HelpButton';
import type { ValidationRulesResponse, OscalModelType } from '@/types/oscal';

const modelTypeLabels: Record<OscalModelType, string> = {
  'catalog': 'Catalog',
  'profile': 'Profile',
  'component-definition': 'Component Definition',
  'system-security-plan': 'System Security Plan',
  'assessment-plan': 'Assessment Plan',
  'assessment-results': 'Assessment Results',
  'plan-of-action-and-milestones': 'Plan of Action & Milestones',
};

const severityIcons = {
  error: <AlertCircle className="h-4 w-4 text-red-500" />,
  warning: <AlertTriangle className="h-4 w-4 text-yellow-500" />,
  info: <Info className="h-4 w-4 text-blue-500" />,
};

export default function ValidationRulesPage() {
  const [rulesData, setRulesData] = useState<ValidationRulesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedModelType, setSelectedModelType] = useState<OscalModelType | 'all'>('all');
  const [selectedCategory, setSelectedCategory] = useState<string | 'all'>('all');
  const [sourceFilter, setSourceFilter] = useState<'all' | 'builtin' | 'custom'>('all');

  useEffect(() => {
    loadRules();
  }, []);

  const loadRules = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getValidationRules();
      setRulesData(data);
      setError(null);
    } catch (err) {
      setError('Failed to load validation rules. Please ensure the backend is running.');
      console.error('Error loading rules:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredRules = rulesData?.rules.filter((rule) => {
    // Search filter
    const matchesSearch = searchQuery === '' ||
      rule.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      rule.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      rule.id.toLowerCase().includes(searchQuery.toLowerCase());

    // Model type filter
    const matchesModel = selectedModelType === 'all' ||
      rule.applicableModelTypes.includes(selectedModelType);

    // Category filter
    const matchesCategory = selectedCategory === 'all' ||
      rule.category === selectedCategory;

    // Source filter (driven by the clickable stats tiles or the dropdown)
    const matchesSource = sourceFilter === 'all'
      || (sourceFilter === 'builtin' && rule.builtIn)
      || (sourceFilter === 'custom' && !rule.builtIn);

    return matchesSearch && matchesModel && matchesCategory && matchesSource;
  }) || [];

  const categories = rulesData?.categories || [];
  const uniqueCategories = categories.map(cat => ({ id: cat.id, name: cat.name }));

  // Statistics show TOTAL counts (independent of source filter) so users always
  // see the full picture in the tiles. The tiles double as source-filter
  // toggles — clicking "Custom Rules" pins sourceFilter='custom'.
  const allRules = rulesData?.rules ?? [];
  const totalStats = {
    total: allRules.length,
    builtIn: allRules.filter(r => r.builtIn).length,
    custom: allRules.filter(r => !r.builtIn).length,
    categories: new Set(allRules.map(r => r.category).filter(Boolean)).size,
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <div className="flex items-center justify-center h-64">
            <div className="text-center">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
              <p className="mt-4 text-muted-foreground">Loading validation rules...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-12 px-4">
          <Link
            href="/"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary mb-8"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Home
          </Link>
          <Card className="border-destructive">
            <CardHeader>
              <CardTitle className="text-destructive">Error Loading Rules</CardTitle>
              <CardDescription>{error}</CardDescription>
            </CardHeader>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4">
        {/* Header */}
        <div className="mb-8">
          <Link
            href="/"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Home
          </Link>
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <ShieldCheck className="h-10 w-10 text-primary" />
                <h1 className="text-4xl font-bold">Validation Rules</h1>
                <HelpButton slug="rules" />
              </div>
              <p className="text-lg text-muted-foreground">
                Explore the validation rules checked for OSCAL documents
              </p>
            </div>
            <Link href="/rules/custom">
              <Button>
                <Settings className="mr-2 h-4 w-4" />
                Manage Custom Rules
              </Button>
            </Link>
          </div>
        </div>

        {/* Statistics Cards — clickable as source filters */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <button
            type="button"
            onClick={() => setSourceFilter('all')}
            aria-pressed={sourceFilter === 'all'}
            className="text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg"
          >
            <Card className={`transition-shadow hover:shadow-md cursor-pointer ${
              sourceFilter === 'all' ? 'ring-2 ring-primary' : ''
            }`}>
              <CardContent className="pt-6">
                <div className="text-2xl font-bold text-primary">{totalStats.total}</div>
                <p className="text-sm text-muted-foreground">All Rules</p>
              </CardContent>
            </Card>
          </button>

          <button
            type="button"
            onClick={() => setSourceFilter(s => s === 'builtin' ? 'all' : 'builtin')}
            aria-pressed={sourceFilter === 'builtin'}
            className="text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded-lg"
          >
            <Card className={`transition-shadow hover:shadow-md cursor-pointer ${
              sourceFilter === 'builtin' ? 'ring-2 ring-blue-500' : ''
            }`}>
              <CardContent className="pt-6">
                <div className="text-2xl font-bold text-blue-500">{totalStats.builtIn}</div>
                <p className="text-sm text-muted-foreground">Built-in Rules</p>
              </CardContent>
            </Card>
          </button>

          <button
            type="button"
            onClick={() => setSourceFilter(s => s === 'custom' ? 'all' : 'custom')}
            aria-pressed={sourceFilter === 'custom'}
            className="text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 rounded-lg"
          >
            <Card className={`transition-shadow hover:shadow-md cursor-pointer ${
              sourceFilter === 'custom' ? 'ring-2 ring-purple-500' : ''
            }`}>
              <CardContent className="pt-6">
                <div className="text-2xl font-bold text-purple-500">{totalStats.custom}</div>
                <p className="text-sm text-muted-foreground">Custom Rules</p>
              </CardContent>
            </Card>
          </button>

          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold text-green-500">{totalStats.categories}</div>
              <p className="text-sm text-muted-foreground">Categories</p>
            </CardContent>
          </Card>
        </div>

        {/* Filters */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <Filter className="h-5 w-5" />
              Filter Rules
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search rules by name, description, or ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Model Type Filter */}
              <div>
                <label className="text-sm font-medium mb-2 block">OSCAL Model Type</label>
                <select
                  value={selectedModelType}
                  onChange={(e) => setSelectedModelType(e.target.value as OscalModelType | 'all')}
                  className="w-full px-3 py-2 border rounded-md bg-background"
                >
                  <option value="all">All Model Types</option>
                  {Object.entries(modelTypeLabels).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </div>

              {/* Category Filter */}
              <div>
                <label className="text-sm font-medium mb-2 block">Category</label>
                <select
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  className="w-full px-3 py-2 border rounded-md bg-background"
                >
                  <option value="all">All Categories</option>
                  {uniqueCategories.map((cat) => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">
                Showing {filteredRules.length} of {rulesData?.totalRules} rules
                {sourceFilter !== 'all' && (
                  <span className="ml-2">
                    · source:{' '}
                    <span className="font-medium text-foreground">
                      {sourceFilter === 'builtin' ? 'built-in' : 'custom'}
                    </span>
                  </span>
                )}
              </span>
              {(searchQuery || selectedModelType !== 'all' || selectedCategory !== 'all' || sourceFilter !== 'all') && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery('');
                    setSelectedModelType('all');
                    setSelectedCategory('all');
                    setSourceFilter('all');
                  }}
                  className="text-primary hover:underline"
                >
                  Clear filters
                </button>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Rules List */}
        <div className="space-y-4">
          {filteredRules.length === 0 ? (
            <Card>
              <CardContent className="pt-6 text-center text-muted-foreground">
                No rules match your current filters. Try adjusting your search criteria.
              </CardContent>
            </Card>
          ) : (
            filteredRules.map((rule) => (
              <Card
                key={rule.id}
                className={`transition-shadow hover:shadow-lg ${
                  !rule.builtIn ? 'cursor-pointer' : ''
                }`}
                onClick={!rule.builtIn ? () => {
                  // Custom rules deep-link to the management page with the
                  // edit dialog pre-opened. Built-in rules aren't editable
                  // so they stay non-clickable.
                  window.location.href = `/rules/custom?edit=${encodeURIComponent(rule.id)}`;
                } : undefined}
                role={!rule.builtIn ? 'link' : undefined}
                tabIndex={!rule.builtIn ? 0 : undefined}
                onKeyDown={!rule.builtIn ? (e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    window.location.href = `/rules/custom?edit=${encodeURIComponent(rule.id)}`;
                  }
                } : undefined}
              >
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <CardTitle className="text-lg flex items-center gap-2">
                        {severityIcons[rule.severity]}
                        {rule.name}
                      </CardTitle>
                      <CardDescription className="mt-2">{rule.description}</CardDescription>
                    </div>
                    <div className="flex items-center gap-2 ml-4">
                      {rule.builtIn ? (
                        <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300">
                          Built-in
                        </span>
                      ) : (
                        <span className="px-2 py-1 text-xs font-medium rounded-full bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300">
                          Custom
                        </span>
                      )}
                      <span className={`px-2 py-1 text-xs font-medium rounded-full capitalize ${
                        rule.severity === 'error' ? 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300' :
                        rule.severity === 'warning' ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300' :
                        'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                      }`}>
                        {rule.severity}
                      </span>
                      {!rule.builtIn && (
                        <ChevronRight
                          className="h-4 w-4 text-muted-foreground"
                          aria-label="Click to manage"
                        />
                      )}
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="font-medium text-muted-foreground">Rule ID:</span>
                      <span className="ml-2 font-mono text-xs">{rule.id}</span>
                    </div>
                    <div>
                      <span className="font-medium text-muted-foreground">Type:</span>
                      <span className="ml-2 capitalize">{rule.ruleType.replace('-', ' ')}</span>
                    </div>
                    <div>
                      <span className="font-medium text-muted-foreground">Category:</span>
                      <span className="ml-2 capitalize">
                        {categories.find(c => c.id === rule.category)?.name || rule.category}
                      </span>
                    </div>
                    {rule.fieldPath && (
                      <div>
                        <span className="font-medium text-muted-foreground">Field Path:</span>
                        <span className="ml-2 font-mono text-xs">{rule.fieldPath}</span>
                      </div>
                    )}
                  </div>
                  {rule.applicableModelTypes.length > 0 && (
                    <div className="mt-4">
                      <span className="font-medium text-muted-foreground text-sm">Applies to:</span>
                      <div className="flex flex-wrap gap-2 mt-2">
                        {rule.applicableModelTypes.map((modelType) => (
                          <span
                            key={modelType}
                            className="px-2 py-1 text-xs rounded-md bg-secondary text-secondary-foreground"
                          >
                            {modelTypeLabels[modelType]}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            ))
          )}
        </div>
      </div>
    </div>
    </ProtectedRoute>
  );
}
