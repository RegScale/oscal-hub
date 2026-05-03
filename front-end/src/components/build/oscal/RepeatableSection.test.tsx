import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { arrayMove } from '@dnd-kit/sortable';
import { RepeatableSection } from './RepeatableSection';

describe('RepeatableSection', () => {
  it('renders an empty-state hint when items is empty', () => {
    render(
      <RepeatableSection
        label="Properties"
        items={[]}
        newItem={() => ({ name: '' })}
        onChange={() => {}}
        renderItem={() => <span>row</span>}
      />,
    );
    expect(screen.getByText(/no properties added yet/i)).toBeInTheDocument();
  });

  it('calls onChange with a new item when Add is clicked', () => {
    const onChange = vi.fn();
    render(
      <RepeatableSection
        label="Things"
        itemLabel="Thing"
        items={[]}
        newItem={() => ({ id: 'new' })}
        onChange={onChange}
        renderItem={() => <span>row</span>}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /add thing/i }));
    expect(onChange).toHaveBeenCalledWith([{ id: 'new' }]);
  });

  it('renders one row per item with the supplied title', () => {
    render(
      <RepeatableSection
        label="Items"
        items={[{ id: 'a' }, { id: 'b' }]}
        newItem={() => ({ id: '' })}
        onChange={() => {}}
        itemTitle={(it) => `Item ${it.id}`}
        renderItem={(it) => <span data-testid={`row-${it.id}`}>{it.id}</span>}
      />,
    );
    expect(screen.getByText('Item a')).toBeInTheDocument();
    expect(screen.getByText('Item b')).toBeInTheDocument();
    expect(screen.getByTestId('row-a')).toBeInTheDocument();
    expect(screen.getByTestId('row-b')).toBeInTheDocument();
  });

  it('removes the right row when its delete button is clicked', () => {
    const onChange = vi.fn();
    render(
      <RepeatableSection
        label="Items"
        items={[{ id: 'a' }, { id: 'b' }, { id: 'c' }]}
        newItem={() => ({ id: '' })}
        onChange={onChange}
        renderItem={(it) => <span>{it.id}</span>}
      />,
    );
    const removeButtons = screen.getAllByRole('button', { name: /remove item/i });
    expect(removeButtons).toHaveLength(3);
    fireEvent.click(removeButtons[1]);
    expect(onChange).toHaveBeenCalledWith([{ id: 'a' }, { id: 'c' }]);
  });

  it('forwards updates from the renderer back through onChange', () => {
    const onChange = vi.fn();
    render(
      <RepeatableSection
        label="Items"
        items={[{ id: 'a' }, { id: 'b' }]}
        newItem={() => ({ id: '' })}
        onChange={onChange}
        renderItem={(_it, _index, update) => (
          <button onClick={() => update({ id: 'updated' })}>edit</button>
        )}
      />,
    );
    const editButtons = screen.getAllByRole('button', { name: 'edit' });
    fireEvent.click(editButtons[0]);
    expect(onChange).toHaveBeenCalledWith([{ id: 'updated' }, { id: 'b' }]);
  });

  it('shows a drag handle for each row', () => {
    render(
      <RepeatableSection
        label="Items"
        itemLabel="Item"
        items={[{ id: 'a' }, { id: 'b' }]}
        newItem={() => ({ id: '' })}
        onChange={() => {}}
        renderItem={(it) => <span>{it.id}</span>}
      />,
    );
    const handles = screen.getAllByRole('button', { name: /drag to reorder/i });
    expect(handles).toHaveLength(2);
  });

  it('omits drag handles when disableReorder is set', () => {
    render(
      <RepeatableSection
        label="Items"
        items={[{ id: 'a' }]}
        newItem={() => ({ id: '' })}
        onChange={() => {}}
        renderItem={(it) => <span>{it.id}</span>}
        disableReorder
      />,
    );
    expect(screen.queryByRole('button', { name: /drag to reorder/i })).toBeNull();
  });

  it('arrayMove (the dnd-kit helper used internally) moves items as expected', () => {
    // The handleDragEnd path in RepeatableSection delegates to arrayMove.
    // dnd-kit's pointer/keyboard sensors require real DOM measurements that
    // happy-dom doesn't fully implement, so we exercise the reorder math
    // directly to lock in the expected behavior.
    const items = [{ id: 'a' }, { id: 'b' }, { id: 'c' }];
    expect(arrayMove(items, 0, 2)).toEqual([{ id: 'b' }, { id: 'c' }, { id: 'a' }]);
    expect(arrayMove(items, 2, 0)).toEqual([{ id: 'c' }, { id: 'a' }, { id: 'b' }]);
  });
});
