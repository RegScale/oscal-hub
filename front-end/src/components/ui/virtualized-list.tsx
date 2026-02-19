'use client';

import { useRef, useCallback, useEffect, useState } from 'react';
import { FixedSizeList as List, ListChildComponentProps } from 'react-window';
import { Loader2 } from 'lucide-react';

/**
 * Props for VirtualizedList component
 */
interface VirtualizedListProps<T> {
  /** Array of items to render */
  items: T[];
  /** Height of each row in pixels */
  itemHeight: number;
  /** Total height of the list container */
  height: number;
  /** Width of the list (default: 100%) */
  width?: number | string;
  /** Render function for each item */
  renderItem: (item: T, index: number, style: React.CSSProperties) => React.ReactNode;
  /** Loading state */
  isLoading?: boolean;
  /** Called when scrolling near the end (for infinite scroll) */
  onEndReached?: () => void;
  /** Threshold for triggering onEndReached (default: 5 items) */
  endReachedThreshold?: number;
  /** Empty state message */
  emptyMessage?: string;
  /** Class name for the list container */
  className?: string;
  /** Overscan count (items to render outside visible area) */
  overscanCount?: number;
}

/**
 * A virtualized list component for rendering large datasets efficiently.
 *
 * Uses react-window to only render visible items, dramatically improving
 * performance for lists with hundreds or thousands of items.
 *
 * Features:
 * - Only renders visible items plus overscan
 * - Supports infinite scroll via onEndReached
 * - Loading states
 * - Empty state handling
 *
 * @example
 * ```tsx
 * <VirtualizedList
 *   items={artifacts}
 *   itemHeight={80}
 *   height={600}
 *   renderItem={(item, index, style) => (
 *     <div style={style}>
 *       <ArtifactCard artifact={item} />
 *     </div>
 *   )}
 *   onEndReached={loadMore}
 *   isLoading={isFetchingNextPage}
 * />
 * ```
 */
export function VirtualizedList<T>({
  items,
  itemHeight,
  height,
  width = '100%',
  renderItem,
  isLoading = false,
  onEndReached,
  endReachedThreshold = 5,
  emptyMessage = 'No items to display',
  className = '',
  overscanCount = 5,
}: VirtualizedListProps<T>) {
  const listRef = useRef<List>(null);
  const [hasCalledEndReached, setHasCalledEndReached] = useState(false);

  // Reset the end reached flag when items change
  useEffect(() => {
    setHasCalledEndReached(false);
  }, [items.length]);

  // Handle scroll to check if we're near the end
  const handleScroll = useCallback(
    ({ scrollOffset }: { scrollOffset: number }) => {
      if (!onEndReached || hasCalledEndReached || isLoading) return;

      const totalHeight = items.length * itemHeight;
      const scrollBottom = scrollOffset + height;
      const threshold = endReachedThreshold * itemHeight;

      if (scrollBottom >= totalHeight - threshold) {
        setHasCalledEndReached(true);
        onEndReached();
      }
    },
    [items.length, itemHeight, height, endReachedThreshold, onEndReached, hasCalledEndReached, isLoading]
  );

  // Row renderer for react-window
  const Row = useCallback(
    ({ index, style }: ListChildComponentProps) => {
      const item = items[index];
      if (!item) return null;
      return <>{renderItem(item, index, style)}</>;
    },
    [items, renderItem]
  );

  // Empty state
  if (!isLoading && items.length === 0) {
    return (
      <div
        className={`flex items-center justify-center text-muted-foreground ${className}`}
        style={{ height }}
      >
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className={className}>
      <List
        ref={listRef}
        height={height}
        width={width}
        itemCount={items.length}
        itemSize={itemHeight}
        onScroll={handleScroll}
        overscanCount={overscanCount}
      >
        {Row}
      </List>

      {/* Loading indicator at bottom */}
      {isLoading && (
        <div className="flex items-center justify-center py-4 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin mr-2" />
          <span className="text-sm">Loading more...</span>
        </div>
      )}
    </div>
  );
}

/**
 * Props for VirtualizedTable component
 */
interface VirtualizedTableProps<T> {
  /** Array of items to render */
  items: T[];
  /** Column definitions */
  columns: {
    key: string;
    header: string;
    width: number | string;
    render: (item: T) => React.ReactNode;
  }[];
  /** Height of each row in pixels */
  rowHeight: number;
  /** Total height of the table container */
  height: number;
  /** Loading state */
  isLoading?: boolean;
  /** Called when scrolling near the end */
  onEndReached?: () => void;
  /** Empty state message */
  emptyMessage?: string;
  /** On row click handler */
  onRowClick?: (item: T, index: number) => void;
}

/**
 * A virtualized table component for rendering large datasets.
 *
 * @example
 * ```tsx
 * <VirtualizedTable
 *   items={auditLogs}
 *   rowHeight={48}
 *   height={500}
 *   columns={[
 *     { key: 'timestamp', header: 'Time', width: 200, render: (item) => item.timestamp },
 *     { key: 'action', header: 'Action', width: 'auto', render: (item) => item.action },
 *   ]}
 * />
 * ```
 */
export function VirtualizedTable<T>({
  items,
  columns,
  rowHeight,
  height,
  isLoading = false,
  onEndReached,
  emptyMessage = 'No data to display',
  onRowClick,
}: VirtualizedTableProps<T>) {
  const headerHeight = 40;

  const renderItem = useCallback(
    (item: T, index: number, style: React.CSSProperties) => (
      <div
        style={{
          ...style,
          display: 'flex',
          alignItems: 'center',
          borderBottom: '1px solid var(--border)',
          cursor: onRowClick ? 'pointer' : 'default',
        }}
        className={`hover:bg-muted/50 ${onRowClick ? 'cursor-pointer' : ''}`}
        onClick={() => onRowClick?.(item, index)}
      >
        {columns.map((col) => (
          <div
            key={col.key}
            style={{
              width: col.width,
              flexGrow: col.width === 'auto' ? 1 : 0,
              flexShrink: 0,
              padding: '0 12px',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {col.render(item)}
          </div>
        ))}
      </div>
    ),
    [columns, onRowClick]
  );

  return (
    <div className="border border-border rounded-md overflow-hidden">
      {/* Header */}
      <div
        className="flex items-center bg-muted/50 border-b border-border font-medium"
        style={{ height: headerHeight }}
      >
        {columns.map((col) => (
          <div
            key={col.key}
            style={{
              width: col.width,
              flexGrow: col.width === 'auto' ? 1 : 0,
              flexShrink: 0,
              padding: '0 12px',
            }}
          >
            {col.header}
          </div>
        ))}
      </div>

      {/* Body */}
      <VirtualizedList
        items={items}
        itemHeight={rowHeight}
        height={height - headerHeight}
        renderItem={renderItem}
        isLoading={isLoading}
        onEndReached={onEndReached}
        emptyMessage={emptyMessage}
      />
    </div>
  );
}

export default VirtualizedList;
