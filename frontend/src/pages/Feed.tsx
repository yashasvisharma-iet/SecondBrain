import { useEffect, useState } from "react";
import axios from "axios";

type NotionDatabase = {
  id: string;
  title: string;
};

export default function Feed() {
  const [databases, setDatabases] = useState<NotionDatabase[]>([]);
  const [loading, setLoading] = useState(true);
  const [inProgress, setInProgress] = useState<Record<string, boolean>>({});
  const [jobIds, setJobIds] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let mounted = true;
    axios
      .get('/api/notion/databases')
      .then((res) => {
        if (!mounted) return;
        // normalize response into array
        const payload = res?.data;
        let list: any[] = [];
        if (Array.isArray(payload)) {
          list = payload;
        } else if (payload && Array.isArray(payload.results)) {
          list = payload.results;
        } else if (payload && typeof payload === 'object') {
          // sometimes backend may wrap or return a single object
          // try to coerce into an array of databases
          // if it looks like a db dto, accept it
          if (payload.id && (payload.title || payload.name)) {
            list = [payload];
          }
        }

        // map/normalize items to NotionDatabase shape
        const normalized = list.map((it) => ({ id: String(it.id), title: String(it.title ?? it.name ?? '(no title)') }));
        setDatabases(normalized);
      })
      .catch(() => {
        if (mounted) setDatabases([]);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, []);

  const ingestDatabase = async (databaseId: string) => {
    setInProgress((s) => ({ ...s, [databaseId]: true }));
    setErrors((e) => ({ ...e, [databaseId]: '' }));

    try {
      const res = await axios.post('/api/ingest/notion', { databaseId });
      const jobId = res.data?.jobId;
      if (jobId) {
        setJobIds((j) => ({ ...j, [databaseId]: jobId }));
        // clear the success after a short while
        setTimeout(() => {
          setJobIds((j) => {
            const copy = { ...j };
            delete copy[databaseId];
            return copy;
          });
        }, 5000);
      } else {
        setErrors((e) => ({ ...e, [databaseId]: 'No job id returned' }));
      }
    } catch (err: any) {
      const msg = err?.response?.data?.error || err?.message || 'Failed to enqueue';
      setErrors((e) => ({ ...e, [databaseId]: String(msg) }));
      // clear error after a bit
      setTimeout(() => {
        setErrors((e) => {
          const copy = { ...e };
          delete copy[databaseId];
          return copy;
        });
      }, 5000);
    } finally {
      setInProgress((s) => ({ ...s, [databaseId]: false }));
    }
  };

  if (loading) return <div className="p-4">Loading Notion databases…</div>;

  const safeDatabases = Array.isArray(databases) ? databases : [];

  return (
    <div className="p-4 space-y-4">
      <h2 className="font-semibold">Select a Notion Database to Ingest</h2>

      {safeDatabases.length === 0 && <div className="text-sm text-gray-500">No databases found.</div>}

      <div className="space-y-2">
        {safeDatabases.map((db) => (
          <div key={db.id} className="flex flex-col">
            <button
              onClick={() => ingestDatabase(db.id)}
              disabled={!!inProgress[db.id]}
              className="w-full border rounded p-2 text-left hover:bg-gray-50 disabled:opacity-50 flex justify-between items-center"
            >
              <span>{db.title}</span>
              {inProgress[db.id] ? (
                <span className="text-xs text-gray-500">Enqueuing…</span>
              ) : (
                <span className="text-xs text-gray-400">Ingest</span>
              )}
            </button>

            <div className="mt-1 pl-2">
              {jobIds[db.id] && (
                <div className="text-xs text-green-600">Job enqueued: {jobIds[db.id]}</div>
              )}
              {errors[db.id] && (
                <div className="text-xs text-red-600">Error: {errors[db.id]}</div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
