'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Plus, Trash2, Edit, CheckCircle, X, AlertCircle } from 'lucide-react';
import type { ConditionType } from '@/types/oscal';
import { DatePicker } from '@/components/ui/date-picker';

export interface Condition {
  id?: number; // Optional for new conditions not yet saved
  condition: string;
  conditionType: ConditionType;
  dueDate?: string;
}

interface ConditionsManagerProps {
  conditions: Condition[];
  onConditionsChange: (conditions: Condition[]) => void;
}

export function ConditionsManager({ conditions, onConditionsChange }: ConditionsManagerProps) {
  const [isAdding, setIsAdding] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [newCondition, setNewCondition] = useState<Condition>({
    condition: '',
    conditionType: 'MANDATORY',
    dueDate: '',
  });

  const handleAddCondition = () => {
    if (!newCondition.condition.trim()) return;

    // Validate mandatory condition has due date
    if (newCondition.conditionType === 'MANDATORY' && !newCondition.dueDate) {
      alert('Mandatory conditions require a due date');
      return;
    }

    onConditionsChange([...conditions, newCondition]);
    setNewCondition({
      condition: '',
      conditionType: 'MANDATORY',
      dueDate: '',
    });
    setIsAdding(false);
  };

  const handleEditCondition = (index: number) => {
    const conditionToEdit = conditions[index];
    setNewCondition({ ...conditionToEdit });
    setEditingIndex(index);
    setIsAdding(true);
  };

  const handleUpdateCondition = () => {
    if (editingIndex === null) return;
    if (!newCondition.condition.trim()) return;

    // Validate mandatory condition has due date
    if (newCondition.conditionType === 'MANDATORY' && !newCondition.dueDate) {
      alert('Mandatory conditions require a due date');
      return;
    }

    const updatedConditions = [...conditions];
    updatedConditions[editingIndex] = newCondition;
    onConditionsChange(updatedConditions);
    setNewCondition({
      condition: '',
      conditionType: 'MANDATORY',
      dueDate: '',
    });
    setEditingIndex(null);
    setIsAdding(false);
  };

  const handleDeleteCondition = (index: number) => {
    const updatedConditions = conditions.filter((_, i) => i !== index);
    onConditionsChange(updatedConditions);
  };

  const handleCancel = () => {
    setNewCondition({
      condition: '',
      conditionType: 'MANDATORY',
      dueDate: '',
    });
    setEditingIndex(null);
    setIsAdding(false);
  };

  const mandatoryCount = conditions.filter(c => c.conditionType === 'MANDATORY').length;
  const recommendedCount = conditions.filter(c => c.conditionType === 'RECOMMENDED').length;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold mb-2">Conditions of Approval</h2>
        <p className="text-gray-600">
          Establish conditions that must be met before or during the authorization period
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-3 gap-4">
        <Card className="p-4 bg-slate-800 border-slate-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-400">Total Conditions</p>
              <p className="text-2xl font-bold">{conditions.length}</p>
            </div>
            <AlertCircle className="h-8 w-8 text-blue-500" />
          </div>
        </Card>

        <Card className="p-4 bg-red-900/20 border-red-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-red-400">Mandatory</p>
              <p className="text-2xl font-bold text-red-400">{mandatoryCount}</p>
            </div>
            <AlertCircle className="h-8 w-8 text-red-500" />
          </div>
        </Card>

        <Card className="p-4 bg-yellow-900/20 border-yellow-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-yellow-400">Recommended</p>
              <p className="text-2xl font-bold text-yellow-400">{recommendedCount}</p>
            </div>
            <AlertCircle className="h-8 w-8 text-yellow-500" />
          </div>
        </Card>
      </div>

      {/* Conditions List */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Label className="text-lg font-semibold">Conditions List</Label>
          {!isAdding && (
            <Button
              type="button"
              onClick={() => setIsAdding(true)}
              size="sm"
              className="flex items-center gap-2"
            >
              <Plus className="h-4 w-4" />
              Add Condition
            </Button>
          )}
        </div>

        {/* Add/Edit Form */}
        {isAdding && (
          <Card className="p-4 border-2 border-blue-500 bg-slate-800">
            <div className="space-y-4">
              <div>
                <Label className="text-sm font-semibold mb-2">
                  {editingIndex !== null ? 'Edit Condition' : 'New Condition'}
                </Label>
              </div>

              <div className="space-y-2">
                <Label htmlFor="condition-type">Condition Type *</Label>
                <select
                  id="condition-type"
                  value={newCondition.conditionType}
                  onChange={(e) =>
                    setNewCondition({
                      ...newCondition,
                      conditionType: e.target.value as ConditionType,
                      dueDate: e.target.value === 'RECOMMENDED' ? '' : newCondition.dueDate,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-md text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="MANDATORY">Mandatory</option>
                  <option value="RECOMMENDED">Recommended</option>
                </select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="condition-text">Condition Description *</Label>
                <Textarea
                  id="condition-text"
                  value={newCondition.condition}
                  onChange={(e) => setNewCondition({ ...newCondition, condition: e.target.value })}
                  placeholder="Enter the condition that must be met..."
                  rows={3}
                  className="resize-none"
                />
              </div>

              {newCondition.conditionType === 'MANDATORY' && (
                <div className="space-y-2">
                  <Label htmlFor="due-date">Due Date *</Label>
                  <DatePicker
                    id="due-date"
                    value={newCondition.dueDate || ''}
                    onChange={(value) => setNewCondition({ ...newCondition, dueDate: value })}
                    placeholder="Select due date"
                  />
                  <p className="text-xs text-slate-400">Required for mandatory conditions</p>
                </div>
              )}

              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  onClick={editingIndex !== null ? handleUpdateCondition : handleAddCondition}
                  size="sm"
                  className="flex items-center gap-2"
                >
                  <CheckCircle className="h-4 w-4" />
                  {editingIndex !== null ? 'Update' : 'Add'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleCancel}
                  size="sm"
                  className="flex items-center gap-2"
                >
                  <X className="h-4 w-4" />
                  Cancel
                </Button>
              </div>
            </div>
          </Card>
        )}

        {/* Existing Conditions */}
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {conditions.length === 0 ? (
            <Card className="p-6 text-center bg-slate-800/50 border-slate-700">
              <p className="text-slate-400">
                No conditions added yet. Click &quot;Add Condition&quot; to get started.
              </p>
            </Card>
          ) : (
            conditions.map((condition, index) => (
              <Card
                key={index}
                className={`p-4 transition-all ${
                  condition.conditionType === 'MANDATORY'
                    ? 'bg-red-900/10 border-red-800'
                    : 'bg-yellow-900/10 border-yellow-800'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 space-y-2">
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={condition.conditionType === 'MANDATORY' ? 'destructive' : 'default'}
                        className={
                          condition.conditionType === 'MANDATORY'
                            ? 'bg-red-600 text-white'
                            : 'bg-yellow-600 text-white'
                        }
                      >
                        {condition.conditionType}
                      </Badge>
                      {condition.dueDate && (
                        <Badge variant="outline" className="text-xs">
                          Due: {new Date(condition.dueDate).toLocaleDateString()}
                        </Badge>
                      )}
                    </div>
                    <p className="text-sm">{condition.condition}</p>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => handleEditCondition(index)}
                      className="h-8 w-8 p-0"
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => handleDeleteCondition(index)}
                      className="h-8 w-8 p-0 text-red-400 hover:bg-red-900/20"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </Card>
            ))
          )}
        </div>
      </div>

      {/* Info Box */}
      <Card className="p-4 bg-blue-900/20 border-blue-800">
        <div className="flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-blue-400 mt-0.5" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-blue-400 mb-1">About Conditions of Approval</p>
            <p className="text-xs text-slate-300">
              <strong>Mandatory:</strong> Conditions that must be completed by the specified due date.
              Failure to meet these may affect the authorization status.
            </p>
            <p className="text-xs text-slate-300 mt-1">
              <strong>Recommended:</strong> Suggested conditions that would improve the security posture,
              but are not required for maintaining authorization.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
