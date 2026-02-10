import * as React from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"

export function NoteEditor({ selectedNodeId }: { selectedNodeId: string | null }) {
  const textareaRef = React.useRef<HTMLTextAreaElement>(null)

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto"
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`
    }
  }

  if (!selectedNodeId) {
    return (
      <div className="flex h-[200px] items-center justify-center rounded-lg border border-dashed text-sm text-muted-foreground">
        Select a note from the graph or create a new one.
      </div>
    )
  }

  return (
    <Card className="border-none shadow-none">
      <CardHeader className="px-4 py-2">
        <CardTitle className="text-lg">Edit Note</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 px-4">
        <Input 
          placeholder="Note title" 
          className="text-base font-semibold border-none px-0 focus-visible:ring-0" 
        />
        <Textarea
          ref={textareaRef}
          onChange={handleChange} // Changed from onInput to onChange
          placeholder="Write your note..."
          className="min-h-[200px] resize-none border-none px-0 text-sm focus-visible:ring-0"
        />
      </CardContent>
      <CardFooter className="flex justify-between border-t px-4 py-3">
        <p className="text-xs text-muted-foreground">Last saved: Just now</p>
        <Button size="sm">Save Note</Button>
      </CardFooter>
    </Card>
  )
}
