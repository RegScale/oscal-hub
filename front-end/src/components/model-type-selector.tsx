'use client';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { OscalModelType } from '@/types/oscal';

interface ModelTypeSelectorProps {
  value: OscalModelType | '';
  onChange: (value: OscalModelType) => void;
  disabled?: boolean;
}

const MODEL_TYPES: { value: OscalModelType; label: string; description: string }[] = [
  {
    value: 'catalog',
    label: 'Catalog',
    description: 'Collection of security controls',
  },
  {
    value: 'profile',
    label: 'Profile',
    description: 'Tailored set of controls from catalogs',
  },
  {
    value: 'component-definition',
    label: 'Component Definition',
    description: 'Description of system components',
  },
  {
    value: 'system-security-plan',
    label: 'System Security Plan',
    description: 'SSP documenting control implementation',
  },
  {
    value: 'assessment-plan',
    label: 'Assessment Plan',
    description: 'Plan for assessing controls',
  },
  {
    value: 'assessment-results',
    label: 'Assessment Results',
    description: 'Results from control assessments',
  },
  {
    value: 'plan-of-action-and-milestones',
    label: 'POA&M',
    description: 'Plan of Action and Milestones',
  },
];

export function ModelTypeSelector({ value, onChange, disabled }: ModelTypeSelectorProps) {
  return (
    <div className="space-y-2">
      <label htmlFor="model-type-select" className="text-sm font-medium">
        OSCAL Model Type
      </label>
      <Select value={value} onValueChange={onChange} disabled={disabled}>
        <SelectTrigger className="w-full" id="model-type-select">
          <SelectValue placeholder="Select model type..." />
        </SelectTrigger>
        <SelectContent>
          {MODEL_TYPES.map((type) => (
            <SelectItem key={type.value} value={type.value}>
              <div className="flex flex-col">
                <span className="font-medium">{type.label}</span>
                <span className="text-xs text-muted-foreground">{type.description}</span>
              </div>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
