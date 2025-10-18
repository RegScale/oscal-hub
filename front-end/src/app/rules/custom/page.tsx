'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  ArrowLeft,
  Plus,
  Edit,
  Trash2,
  AlertCircle,
  Loader2,
  Search,
} from 'lucide-react';
import { apiClient } from '@/lib/api-client';
import ProtectedRoute from '@/components/ProtectedRoute';
import type {
  CustomRuleResponse,
  CustomRuleRequest,
  OscalModelType,
  ValidationRuleType,
  ValidationRuleSeverity,
} from '@/types/oscal';

const RULE_TYPES: ValidationRuleType[] = [
  'required-field',
  'pattern-match',
  'allowed-values',
  'cardinality',
  'cross-field',
  'id-reference',
  'data-type',
  'custom',
];

const SEVERITIES: ValidationRuleSeverity[] = ['error', 'warning', 'info'];

const MODEL_TYPES: OscalModelType[] = [
  'catalog',
  'profile',
  'component-definition',
  'system-security-plan',
  'assessment-plan',
  'assessment-results',
  'plan-of-action-and-milestones',
];

const CATEGORIES = [
  'metadata',
  'security-controls',
  'identifiers',
  'references',
  'structural',
  'profile',
  'component',
  'ssp',
  'assessment',
  'custom',
];

export default function CustomRulesPage() {
  const [customRules, setCustomRules] = useState<CustomRuleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [filterEnabled, setFilterEnabled] = useState<string>('all');

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<CustomRuleResponse | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [ruleToDelete, setRuleToDelete] = useState<CustomRuleResponse | null>(null);

  const [formData, setFormData] = useState<CustomRuleRequest>({
    ruleId: '',
    name: '',
    description: '',
    ruleType: 'required-field',
    severity: 'error',
    category: 'custom',
    fieldPath: '',
    ruleExpression: '',
    constraintDetails: '',
    applicableModelTypes: [],
    enabled: true,
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadCustomRules();
  }, []);

  const loadCustomRules = async () => {
    try {
      setLoading(true);
      setError(null);
      const rules = await apiClient.getAllCustomRules();
      setCustomRules(rules);
    } catch (err) {
      setError('Failed to load custom rules. Please try again.');
      console.error('Error loading custom rules:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateNew = () => {
    setEditingRule(null);
    setFormData({
      ruleId: '',
      name: '',
      description: '',
      ruleType: 'required-field',
      severity: 'error',
      category: 'custom',
      fieldPath: '',
      ruleExpression: '',
      constraintDetails: '',
      applicableModelTypes: [],
      enabled: true,
    });
    setFormError(null);
    setDialogOpen(true);
  };

  const handleEdit = (rule: CustomRuleResponse) => {
    setEditingRule(rule);
    setFormData({
      ruleId: rule.ruleId,
      name: rule.name,
      description: rule.description || '',
      ruleType: rule.ruleType,
      severity: rule.severity,
      category: rule.category || 'custom',
      fieldPath: rule.fieldPath || '',
      ruleExpression: rule.ruleExpression || '',
      constraintDetails: rule.constraintDetails || '',
      applicableModelTypes: rule.applicableModelTypes || [],
      enabled: rule.enabled,
    });
    setFormError(null);
    setDialogOpen(true);
  };

  const handleDelete = (rule: CustomRuleResponse) => {
    setRuleToDelete(rule);
    setDeleteDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (!ruleToDelete) return;

    try {
      await apiClient.deleteCustomRule(ruleToDelete.id);
      await loadCustomRules();
      setDeleteDialogOpen(false);
      setRuleToDelete(null);
    } catch (err) {
      console.error('Error deleting rule:', err);
      alert('Failed to delete rule. Please try again.');
    }
  };

  const handleToggleEnabled = async (rule: CustomRuleResponse) => {
    try {
      await apiClient.toggleCustomRuleEnabled(rule.id);
      await loadCustomRules();
    } catch (err) {
      console.error('Error toggling rule:', err);
      alert('Failed to toggle rule. Please try again.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);

    try {
      if (editingRule) {
        await apiClient.updateCustomRule(editingRule.id, formData);
      } else {
        await apiClient.createCustomRule(formData);
      }

      await loadCustomRules();
      setDialogOpen(false);
    } catch (err: unknown) {
      setFormError(err instanceof Error ? err.message : 'Failed to save rule. Please try again.');
      console.error('Error saving rule:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleModelType = (modelType: string) => {
    const current = formData.applicableModelTypes || [];
    if (current.includes(modelType)) {
      setFormData({
        ...formData,
        applicableModelTypes: current.filter((t) => t !== modelType),
      });
    } else {
      setFormData({
        ...formData,
        applicableModelTypes: [...current, modelType],
      });
    }
  };

  const filteredRules = customRules.filter((rule) => {
    const matchesSearch =
      searchQuery === '' ||
      rule.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      rule.ruleId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (rule.description && rule.description.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesCategory =
      filterCategory === 'all' || rule.category === filterCategory;

    const matchesEnabled =
      filterEnabled === 'all' ||
      (filterEnabled === 'enabled' && rule.enabled) ||
      (filterEnabled === 'disabled' && !rule.enabled);

    return matchesSearch && matchesCategory && matchesEnabled;
  });

  const getSeverityColor = (severity: string): string => {
    switch (severity.toLowerCase()) {
      case 'error':
        return 'bg-red-100 text-red-800 border-red-200 dark:bg-red-900 dark:text-red-300 dark:border-red-800';
      case 'warning':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900 dark:text-yellow-300 dark:border-yellow-800';
      case 'info':
        return 'bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-900 dark:text-blue-300 dark:border-blue-800';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-700';
    }
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/rules">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Back to Rules
              </Button>
            </Link>
            <div>
              <h1 className="text-4xl font-bold text-foreground">Custom Validation Rules</h1>
              <p className="text-muted-foreground mt-2">Create and manage custom validation rules</p>
            </div>
          </div>
          <Button onClick={handleCreateNew}>
            <Plus className="mr-2 h-4 w-4" />
            New Rule
          </Button>
        </div>

        {/* Filters */}
        <Card className="mb-6">
          <CardContent className="pt-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="col-span-2">
                <div className="relative">
                  <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Search rules..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
              <div>
                <Select value={filterCategory} onValueChange={setFilterCategory}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Categories</SelectItem>
                    {CATEGORIES.map((cat) => (
                      <SelectItem key={cat} value={cat}>
                        {cat}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Select value={filterEnabled} onValueChange={setFilterEnabled}>
                  <SelectTrigger>
                    <SelectValue placeholder="Filter by status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Status</SelectItem>
                    <SelectItem value="enabled">Enabled Only</SelectItem>
                    <SelectItem value="disabled">Disabled Only</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Error State */}
        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {/* Loading State */}
        {loading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        )}

        {/* Rules List */}
        {!loading && (
          <div className="space-y-4">
            {filteredRules.length === 0 ? (
              <Card>
                <CardContent className="pt-6">
                  <p className="text-center text-muted-foreground">
                    No custom rules found. Create your first rule to get started.
                  </p>
                </CardContent>
              </Card>
            ) : (
              filteredRules.map((rule) => (
                <Card key={rule.id} className="hover:shadow-md transition-shadow">
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <CardTitle className="text-xl">{rule.name}</CardTitle>
                          <Badge
                            variant="outline"
                            className={getSeverityColor(rule.severity)}
                          >
                            {rule.severity.toUpperCase()}
                          </Badge>
                          <Badge variant="outline">{rule.ruleType}</Badge>
                          {rule.category && (
                            <Badge variant="secondary">{rule.category}</Badge>
                          )}
                        </div>
                        <CardDescription>
                          <span className="font-mono text-xs">{rule.ruleId}</span>
                        </CardDescription>
                        {rule.description && (
                          <p className="text-sm text-muted-foreground mt-2">{rule.description}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-2 mr-4">
                          <Label htmlFor={`toggle-${rule.id}`} className="text-sm">
                            {rule.enabled ? 'Enabled' : 'Disabled'}
                          </Label>
                          <Switch
                            id={`toggle-${rule.id}`}
                            checked={rule.enabled}
                            onCheckedChange={() => handleToggleEnabled(rule)}
                          />
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEdit(rule)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(rule)}
                        >
                          <Trash2 className="h-4 w-4 text-red-600" />
                        </Button>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      {rule.fieldPath && (
                        <div>
                          <span className="font-semibold">Field Path:</span>{' '}
                          <span className="font-mono text-xs">{rule.fieldPath}</span>
                        </div>
                      )}
                      {rule.applicableModelTypes && rule.applicableModelTypes.length > 0 && (
                        <div>
                          <span className="font-semibold">Applicable Models:</span>{' '}
                          {rule.applicableModelTypes.join(', ')}
                        </div>
                      )}
                      {rule.ruleExpression && (
                        <div className="col-span-2">
                          <span className="font-semibold">Expression:</span>{' '}
                          <span className="font-mono text-xs">{rule.ruleExpression}</span>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))
            )}
          </div>
        )}

        {/* Create/Edit Dialog */}
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>
                {editingRule ? 'Edit Custom Rule' : 'Create New Custom Rule'}
              </DialogTitle>
              <DialogDescription>
                {editingRule
                  ? 'Update the custom validation rule details below.'
                  : 'Define a new custom validation rule for OSCAL documents.'}
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit}>
              <div className="space-y-4">
                {formError && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{formError}</AlertDescription>
                  </Alert>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="ruleId">Rule ID *</Label>
                    <Input
                      id="ruleId"
                      value={formData.ruleId}
                      onChange={(e) =>
                        setFormData({ ...formData, ruleId: e.target.value })
                      }
                      placeholder="custom-rule-001"
                      required
                      disabled={!!editingRule}
                    />
                  </div>
                  <div>
                    <Label htmlFor="name">Name *</Label>
                    <Input
                      id="name"
                      value={formData.name}
                      onChange={(e) =>
                        setFormData({ ...formData, name: e.target.value })
                      }
                      placeholder="My Custom Rule"
                      required
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="description">Description</Label>
                  <Textarea
                    id="description"
                    value={formData.description}
                    onChange={(e) =>
                      setFormData({ ...formData, description: e.target.value })
                    }
                    placeholder="Describe what this rule validates..."
                    rows={3}
                  />
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <Label htmlFor="ruleType">Rule Type *</Label>
                    <Select
                      value={formData.ruleType}
                      onValueChange={(value) =>
                        setFormData({ ...formData, ruleType: value })
                      }
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {RULE_TYPES.map((type) => (
                          <SelectItem key={type} value={type}>
                            {type}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="severity">Severity *</Label>
                    <Select
                      value={formData.severity}
                      onValueChange={(value) =>
                        setFormData({ ...formData, severity: value })
                      }
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {SEVERITIES.map((sev) => (
                          <SelectItem key={sev} value={sev}>
                            {sev}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="category">Category</Label>
                    <Select
                      value={formData.category || 'custom'}
                      onValueChange={(value) =>
                        setFormData({ ...formData, category: value })
                      }
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {CATEGORIES.map((cat) => (
                          <SelectItem key={cat} value={cat}>
                            {cat}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div>
                  <Label htmlFor="fieldPath">Field Path</Label>
                  <Input
                    id="fieldPath"
                    value={formData.fieldPath}
                    onChange={(e) =>
                      setFormData({ ...formData, fieldPath: e.target.value })
                    }
                    placeholder="/metadata/title"
                  />
                </div>

                <div>
                  <Label htmlFor="ruleExpression">Rule Expression</Label>
                  <Textarea
                    id="ruleExpression"
                    value={formData.ruleExpression}
                    onChange={(e) =>
                      setFormData({ ...formData, ruleExpression: e.target.value })
                    }
                    placeholder="JSONPath, XPath, or custom expression..."
                    rows={2}
                  />
                </div>

                <div>
                  <Label htmlFor="constraintDetails">Constraint Details</Label>
                  <Textarea
                    id="constraintDetails"
                    value={formData.constraintDetails}
                    onChange={(e) =>
                      setFormData({ ...formData, constraintDetails: e.target.value })
                    }
                    placeholder="Additional constraint details..."
                    rows={2}
                  />
                </div>

                <div>
                  <Label>Applicable Model Types</Label>
                  <div className="grid grid-cols-2 gap-2 mt-2">
                    {MODEL_TYPES.map((modelType) => (
                      <div key={modelType} className="flex items-center space-x-2">
                        <input
                          type="checkbox"
                          id={`model-${modelType}`}
                          checked={formData.applicableModelTypes?.includes(modelType)}
                          onChange={() => toggleModelType(modelType)}
                          className="rounded border-input"
                        />
                        <label
                          htmlFor={`model-${modelType}`}
                          className="text-sm cursor-pointer"
                        >
                          {modelType}
                        </label>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <Switch
                    id="enabled"
                    checked={formData.enabled}
                    onCheckedChange={(checked) =>
                      setFormData({ ...formData, enabled: checked })
                    }
                  />
                  <Label htmlFor="enabled">Enable this rule</Label>
                </div>
              </div>

              <DialogFooter className="mt-6">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setDialogOpen(false)}
                  disabled={submitting}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={submitting}>
                  {submitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      {editingRule ? 'Updating...' : 'Creating...'}
                    </>
                  ) : (
                    <>{editingRule ? 'Update Rule' : 'Create Rule'}</>
                  )}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>

        {/* Delete Confirmation Dialog */}
        <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Delete Custom Rule</DialogTitle>
              <DialogDescription>
                Are you sure you want to delete &quot;{ruleToDelete?.name}&quot;? This
                action cannot be undone.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setDeleteDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button variant="destructive" onClick={confirmDelete}>
                Delete Rule
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
    </ProtectedRoute>
  );
}
