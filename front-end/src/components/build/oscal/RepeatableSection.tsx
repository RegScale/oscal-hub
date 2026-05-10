'use client';

import { ReactNode } from 'react';
import { Button } from '@/components/ui/button';
import { Plus, Trash2, GripVertical } from 'lucide-react';
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

interface RepeatableSectionProps<T> {
  label: string;
  itemLabel?: string;
  description?: string;
  items: T[];
  newItem: () => T;
  renderItem: (item: T, index: number, update: (next: T) => void) => ReactNode;
  onChange: (next: T[]) => void;
  itemTitle?: (item: T, index: number) => string;
  /** When true, drag-and-drop reordering is disabled. */
  disableReorder?: boolean;
}

/**
 * Generic add/remove/reorder helper used everywhere. Reorder via drag handle
 * (mouse + keyboard via dnd-kit) — Space to pick up, arrows to move, Space to drop.
 */
export function RepeatableSection<T>({
  label,
  itemLabel = 'Item',
  description,
  items,
  newItem,
  renderItem,
  onChange,
  itemTitle,
  disableReorder = false,
}: RepeatableSectionProps<T>) {
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const update = (index: number, next: T) => {
    const copy = [...items];
    copy[index] = next;
    onChange(copy);
  };
  const remove = (index: number) => {
    onChange(items.filter((_, i) => i !== index));
  };

  const ids = items.map((_, i) => `item-${i}`);

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = ids.indexOf(String(active.id));
    const newIndex = ids.indexOf(String(over.id));
    if (oldIndex < 0 || newIndex < 0) return;
    onChange(arrayMove(items, oldIndex, newIndex));
  };

  return (
    <div className="space-y-3">
      <div className="flex items-end justify-between">
        <div>
          <h4 className="text-sm font-semibold">{label}</h4>
          {description && (
            <p className="text-xs text-muted-foreground">{description}</p>
          )}
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange([...items, newItem()])}
        >
          <Plus className="h-3.5 w-3.5 mr-1" />
          Add {itemLabel}
        </Button>
      </div>

      {items.length === 0 ? (
        <p className="text-xs text-muted-foreground italic px-3 py-4 border border-dashed rounded-md text-center">
          No {label.toLowerCase()} added yet.
        </p>
      ) : disableReorder ? (
        <div className="space-y-3">
          {items.map((item, index) => (
            <Row
              key={index}
              dragId={ids[index]}
              dragDisabled
              itemLabel={itemLabel}
              title={itemTitle ? itemTitle(item, index) : `${itemLabel} ${index + 1}`}
              onRemove={() => remove(index)}
            >
              {renderItem(item, index, (next) => update(index, next))}
            </Row>
          ))}
        </div>
      ) : (
        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
          <SortableContext items={ids} strategy={verticalListSortingStrategy}>
            <div className="space-y-3">
              {items.map((item, index) => (
                <SortableRow
                  key={ids[index]}
                  dragId={ids[index]}
                  itemLabel={itemLabel}
                  title={itemTitle ? itemTitle(item, index) : `${itemLabel} ${index + 1}`}
                  onRemove={() => remove(index)}
                >
                  {renderItem(item, index, (next) => update(index, next))}
                </SortableRow>
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}
    </div>
  );
}

interface RowProps {
  dragId: string;
  itemLabel: string;
  title: string;
  onRemove: () => void;
  children: ReactNode;
  dragDisabled?: boolean;
}

function Row({ title, onRemove, children, dragDisabled }: RowProps) {
  return (
    <div className="rounded-md border bg-card p-3 space-y-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1 min-w-0">
          {!dragDisabled && (
            <span className="text-muted-foreground/40">
              <GripVertical className="h-3.5 w-3.5" />
            </span>
          )}
          <span className="text-xs font-medium text-muted-foreground truncate">{title}</span>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onRemove}
          className="h-7 w-7 p-0 text-destructive hover:text-destructive"
          aria-label="Remove item"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </Button>
      </div>
      {children}
    </div>
  );
}

function SortableRow(props: RowProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: props.dragId,
  });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.6 : 1,
  };
  return (
    <div ref={setNodeRef} style={style} className="rounded-md border bg-card p-3 space-y-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1 min-w-0">
          <button
            type="button"
            {...attributes}
            {...listeners}
            className="cursor-grab active:cursor-grabbing text-muted-foreground hover:text-foreground p-0.5 -ml-1"
            aria-label={`Drag to reorder ${props.itemLabel}`}
          >
            <GripVertical className="h-3.5 w-3.5" />
          </button>
          <span className="text-xs font-medium text-muted-foreground truncate">{props.title}</span>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={props.onRemove}
          className="h-7 w-7 p-0 text-destructive hover:text-destructive"
          aria-label="Remove item"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </Button>
      </div>
      {props.children}
    </div>
  );
}
