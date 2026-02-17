"use client"

import * as React from "react"
import { format, parse, isValid } from "date-fns"
import { Calendar as CalendarIcon } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Calendar } from "@/components/ui/calendar"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

interface DatePickerProps {
  id?: string
  value?: string
  onChange?: (value: string) => void
  placeholder?: string
  className?: string
  disabled?: boolean
  minDate?: string
}

// Try to parse a date string in multiple formats
function parseDate(dateStr: string): Date | undefined {
  if (!dateStr || dateStr.trim() === '') return undefined

  // Clean up the input
  const cleaned = dateStr.trim()

  // Try various common formats
  const formats = [
    'yyyy-MM-dd',    // ISO format: 2025-01-15
    'MM/dd/yyyy',    // US format: 01/15/2025
    'M/d/yyyy',      // US format short: 1/15/2025
    'MM-dd-yyyy',    // US with dashes: 01-15-2025
    'M-d-yyyy',      // US with dashes short: 1-15-2025
    'dd/MM/yyyy',    // EU format: 15/01/2025
    'd/M/yyyy',      // EU format short: 15/1/2025
    'MMM d, yyyy',   // Month name: Jan 15, 2025
    'MMMM d, yyyy',  // Full month name: January 15, 2025
    'MMM dd, yyyy',  // Month name: Jan 15, 2025
    'MMMM dd, yyyy', // Full month name: January 15, 2025
  ]

  for (const fmt of formats) {
    try {
      const parsed = parse(cleaned, fmt, new Date())
      if (isValid(parsed)) {
        return parsed
      }
    } catch {
      // Continue to next format
    }
  }

  // Try native Date parsing as fallback
  const nativeParsed = new Date(cleaned)
  if (isValid(nativeParsed) && !isNaN(nativeParsed.getTime())) {
    return nativeParsed
  }

  return undefined
}

export function DatePicker({
  id,
  value,
  onChange,
  placeholder = "MM/DD/YYYY",
  className,
  disabled,
  minDate,
}: DatePickerProps) {
  const [date, setDate] = React.useState<Date | undefined>(
    value ? new Date(value) : undefined
  )
  const [inputValue, setInputValue] = React.useState<string>(
    value ? format(new Date(value), 'MM/dd/yyyy') : ''
  )
  const [isOpen, setIsOpen] = React.useState(false)

  React.useEffect(() => {
    if (value) {
      const newDate = new Date(value)
      setDate(newDate)
      setInputValue(format(newDate, 'MM/dd/yyyy'))
    } else {
      setDate(undefined)
      setInputValue('')
    }
  }, [value])

  const handleSelect = (selectedDate: Date | undefined) => {
    setDate(selectedDate)
    if (selectedDate && onChange) {
      setInputValue(format(selectedDate, 'MM/dd/yyyy'))
      // Format as YYYY-MM-DD for compatibility with existing code
      const formatted = format(selectedDate, "yyyy-MM-dd")
      onChange(formatted)
    } else if (!selectedDate && onChange) {
      setInputValue('')
      onChange("")
    }
    setIsOpen(false)
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value
    setInputValue(newValue)
  }

  const handleInputBlur = () => {
    if (!inputValue || inputValue.trim() === '') {
      setDate(undefined)
      if (onChange) {
        onChange('')
      }
      return
    }

    const parsed = parseDate(inputValue)
    if (parsed) {
      // Check minDate constraint
      const minDateObj = minDate ? new Date(minDate) : undefined
      if (minDateObj && parsed < minDateObj) {
        // If date is before minDate, use minDate instead
        setDate(minDateObj)
        setInputValue(format(minDateObj, 'MM/dd/yyyy'))
        if (onChange) {
          onChange(format(minDateObj, 'yyyy-MM-dd'))
        }
      } else {
        setDate(parsed)
        setInputValue(format(parsed, 'MM/dd/yyyy'))
        if (onChange) {
          onChange(format(parsed, 'yyyy-MM-dd'))
        }
      }
    } else {
      // Reset to previous valid value if parsing failed
      if (date) {
        setInputValue(format(date, 'MM/dd/yyyy'))
      } else {
        setInputValue('')
      }
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleInputBlur()
    }
  }

  const minDateObj = minDate ? new Date(minDate) : undefined

  return (
    <div className={cn("flex gap-1", className)}>
      <Input
        id={id}
        type="text"
        value={inputValue}
        onChange={handleInputChange}
        onBlur={handleInputBlur}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        className="flex-1"
      />
      <Popover open={isOpen} onOpenChange={setIsOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            size="icon"
            disabled={disabled}
            className="shrink-0"
            type="button"
          >
            <CalendarIcon className="h-4 w-4" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="end">
          <Calendar
            mode="single"
            selected={date}
            onSelect={handleSelect}
            disabled={minDateObj ? { before: minDateObj } : undefined}
            defaultMonth={date}
            initialFocus
          />
        </PopoverContent>
      </Popover>
    </div>
  )
}
