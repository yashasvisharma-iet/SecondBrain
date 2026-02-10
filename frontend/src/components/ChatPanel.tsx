// ChatPanel.tsx
export function ChatPanel({ selectedNodeId }: { selectedNodeId: string | null }) {
  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 p-4 space-y-3 overflow-auto text-sm">
        <div className="text-gray-500">
          {selectedNodeId
            ? `Chatting with context from ${selectedNodeId}`
            : 'General AI chat'}
        </div>

        {/* Messages */}
        <div className="bg-gray-100 p-2 rounded">
          How can I help with this note?
        </div>
      </div>

      <div className="border-t p-3">
        <input
          className="w-full border rounded px-3 py-2 text-sm"
          placeholder="Ask AI about this note..."
        />
      </div>
    </div>
  )
}
