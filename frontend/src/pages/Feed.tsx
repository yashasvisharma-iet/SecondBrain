import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

type FeedItem = {
  id: string;
  title: string;
  body: string;
  createdAt: string;
};

async function fetchFeed(): Promise<FeedItem[]> {
  const res = await fetch("/api/feed", {
    credentials: "include",
  });

  if (!res.ok) {
    throw new Error("Failed to load feed");
  }

  return res.json();
}

const Feed = () => {
  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["feed"],
    queryFn: fetchFeed,
    staleTime: 30_000, // feed can be slightly stale
    retry: 2,
  });

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
        <p className="text-sm text-destructive">
          {(error as Error).message}
        </p>
        <button
          onClick={() => refetch()}
          className="text-sm font-medium underline underline-offset-4"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
        No items yet
      </div>
    );
  }

  return (
    <main className="flex flex-col gap-4 px-4 py-6 pb-24">
      {data.map((item) => (
        <article
          key={item.id}
          className={cn(
            "rounded-xl border bg-background p-4 shadow-sm",
            "transition-colors hover:bg-muted/50"
          )}
        >
          <header className="mb-2">
            <h2 className="text-sm font-semibold leading-tight">
              {item.title}
            </h2>
            <time
              className="text-xs text-muted-foreground"
              dateTime={item.createdAt}
            >
              {new Date(item.createdAt).toLocaleString()}
            </time>
          </header>

          <p className="text-sm text-foreground/90 whitespace-pre-wrap">
            {item.body}
          </p>
        </article>
      ))}
    </main>
  );
};

export default Feed;
