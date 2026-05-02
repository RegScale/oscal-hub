import React from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface EmptyStateAction {
  label: string;
  onClick: () => void;
}

interface EmptyStateProps {
  title: string;
  description?: string;
  primary: EmptyStateAction;
  secondary?: EmptyStateAction;
}

export function EmptyState({ title, description, primary, secondary }: EmptyStateProps) {
  return (
    <Card className="max-w-2xl mx-auto mt-12">
      <CardContent className="p-8 text-center space-y-4">
        <h2 className="text-2xl font-semibold">{title}</h2>
        {description && <p className="text-muted-foreground">{description}</p>}
        <div className="flex gap-3 justify-center pt-2">
          <Button onClick={primary.onClick}>{primary.label}</Button>
          {secondary && (
            <Button variant="outline" onClick={secondary.onClick}>
              {secondary.label}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
